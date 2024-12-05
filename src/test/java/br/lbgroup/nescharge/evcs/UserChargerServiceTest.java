package br.lbgroup.nescharge.evcs;

import br.lbgroup.nescharge.evcs.chargingdata.ChargingData;
import br.lbgroup.nescharge.evcs.chargingdata.ChargingDataRelationsService;
import br.lbgroup.nescharge.evcs.model.Address;
import br.lbgroup.nescharge.evcs.model.ChargeEventType;
import br.lbgroup.nescharge.evcs.model.Charger;
import br.lbgroup.nescharge.evcs.model.OperationMode;
import br.simplipark.evcs.model.*;
import br.lbgroup.nescharge.payment.PaymentService;
import br.lbgroup.nescharge.payment.isolated.PricingRecord;
import br.lbgroup.nescharge.payment.isolated.PricingRecordRepository;
import br.lbgroup.nescharge.payment.model.Payment;
import br.lbgroup.nescharge.payment.model.PaymentReason;
import br.lbgroup.nescharge.test.TestUtils;
import br.lbgroup.commons.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserChargerServiceTest {

    private ChargerService chargerService;
    private PaymentService paymentService;
    private ChargingDataRelationsService chargingDataRelationsService;
    private PricingRecordRepository pricingRecordRepository;

    private UserChargerService userChargerService;

    private User user;
    private Charger charger;
    private ChargingData chargingData;
    private PricingRecord pricingRecord;

    @BeforeEach
    void setUp() {
        chargerService = mock(ChargerService.class);
        paymentService = mock(PaymentService.class);
        chargingDataRelationsService = mock(ChargingDataRelationsService.class);
        pricingRecordRepository = mock(PricingRecordRepository.class);

        userChargerService = new UserChargerService(chargerService, paymentService, chargingDataRelationsService, pricingRecordRepository);

        user = TestUtils.createSampleUser();
        charger = createDummyCharger();
        chargingData = createSampleChargingData();

        pricingRecord = new PricingRecord();
        pricingRecord.setMultiplicatorPrices(0.5F);
    }

    @Test
    void startChargingSuccessfully_onStopChargingAutomaticallyExecutedOnce() {
        ChargingData chargingDataWithId = chargingDataWithId();

        mockDependenciesToStartAndStopChargingSucessfully(chargingDataWithId);

        Runnable onStopChargingAutomatically = mock(Runnable.class);

        doAnswer(invocation -> {
            Consumer<ChargingData> callback = invocation.getArgument(1);

            // Simulate the callback being triggered twice
            callback.accept(chargingDataWithId);
            assertThrows(IllegalStateException.class, () -> callback.accept(chargingDataWithId));

            return null;
        }).when(chargerService).onStopChargingAutomatically(eq(charger), any());

        boolean result = userChargerService.startCharging(user, charger, onStopChargingAutomatically);

        assertTrue(result);
        verify(chargerService).startCharging(charger);
        verify(chargingDataRelationsService).createRelationBetweenUserAndChargingData(user, chargingDataWithId);

        // Verify that the onStopChargingAutomatically Runnable is run once
        verify(onStopChargingAutomatically, times(1)).run();
    }

    @Test
    void startCharging_AlreadyCharging() {
        when(chargerService.startCharging(charger)).thenReturn(chargingDataWithId());

        userChargerService.startCharging(user, charger, () -> {
        });

        assertThrows(IllegalStateException.class, () -> userChargerService.startCharging(user, charger, () -> {
        }));
        verify(chargerService, times(1)).startCharging(charger);
    }

    @Test
    void startCharging_FailedToStartCharging() {
        when(chargerService.startCharging(charger)).thenReturn(null);

        boolean result = userChargerService.startCharging(user, charger, () -> {
        });

        assertFalse(result);
        verify(chargerService).startCharging(charger);
    }

    @Test
    void stopCharging_NotCharging() {
        assertThrows(IllegalStateException.class, () -> userChargerService.stopCharging(user));
        verify(chargerService, never()).stopCharging(any());
    }

    @Test
    void startAndStopCharging_ThenCheckForDuePayment() {
        ChargingData chargingDataWithId = chargingDataWithId();

        mockDependenciesToStartAndStopChargingSucessfully(chargingDataWithId);

        boolean startResult = userChargerService.startCharging(user, charger, () -> {
        });
        assertTrue(startResult);

        userChargerService.stopCharging(user);

        verify(chargerService).startCharging(charger);
        verify(chargingDataRelationsService).createRelationBetweenUserAndChargingData(user, chargingDataWithId);
        verify(chargerService).stopCharging(charger);
        verify(chargingDataRelationsService).updateChargingDataWithNewMeasurements(anyLong(), eq(chargingData));
        verify(paymentService).addPayment(any(Payment.class));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentService).addPayment(paymentCaptor.capture());

        Payment capturedPayment = paymentCaptor.getValue();
        assertEquals(user.id(), capturedPayment.getUserId());
        assertEquals(PaymentReason.EV_CHARGE, capturedPayment.getReason());
        assertEquals(String.valueOf(chargingDataWithId.getId()), capturedPayment.getReasonData());
        assertEquals(5.0, capturedPayment.getAmount());
    }

    @Test
    void shouldCallListenersCorrectly() {
        ChargingData chargingDataWithId = chargingDataWithId();

        mockDependenciesToStartAndStopChargingSucessfully(chargingDataWithId);

        AtomicBoolean hasStartedListenerExecuted = new AtomicBoolean(false);
        registerListenerWithAssertions(chargingDataWithId, hasStartedListenerExecuted, ChargeEventType.STARTED);

        AtomicBoolean hasStoppedListenerExecuted = new AtomicBoolean(false);
        registerListenerWithAssertions(chargingDataWithId, hasStoppedListenerExecuted, ChargeEventType.STOPPED);

        userChargerService.startCharging(user, charger, () -> {
        });

        assertTrue(hasStartedListenerExecuted.get());

        userChargerService.stopCharging(user);

        assertTrue(hasStoppedListenerExecuted.get());
    }

    private void registerListenerWithAssertions(ChargingData chargingDataWithId, AtomicBoolean hasStoppedListenerExecuted, ChargeEventType stopped) {
        userChargerService.addListener(chargeEvent -> {
            assertEquals(user, chargeEvent.user());
            assertEquals(charger, chargeEvent.charger());
            assertEquals(chargingDataWithId, chargeEvent.chargingData());

            hasStoppedListenerExecuted.set(stopped == chargeEvent.type());
        });
    }

    private void mockDependenciesToStartAndStopChargingSucessfully(ChargingData chargingDataWithId) {
        when(chargerService.startCharging(charger)).thenReturn(chargingDataWithId);
        when(chargingDataRelationsService.findChargerIdByChargingDataId(anyLong())).thenReturn(1L);
        when(chargerService.findChargerById(anyLong())).thenReturn(charger);
        when(chargerService.stopCharging(charger)).thenReturn(chargingData);
        when(chargingDataRelationsService.updateChargingDataWithNewMeasurements(anyLong(), any())).thenReturn(chargingDataWithId);
        when(pricingRecordRepository.findByTypeClientPricesAndOwnerPrices(user.type(), charger.owner())).thenReturn(pricingRecord);
        when(paymentService.addPayment(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ChargingData chargingDataWithId() {
        ChargingData chargingDataWithId = createSampleChargingData();
        chargingDataWithId.setId(1L);

        return chargingDataWithId;
    }

    private ChargingData createSampleChargingData() {
        return new ChargingData(10000, LocalDateTime.now().minusHours(1), LocalDateTime.now());
    }

    private Charger createDummyCharger() {
        Address address = new Address("123 Main St", "SomeCity", "12345", "72231216");
        return new Charger("Charger1", "Position1", address, "Owner1", OperationMode.AUTOMATIC_OCPP, new HashMap<>());
    }
}
