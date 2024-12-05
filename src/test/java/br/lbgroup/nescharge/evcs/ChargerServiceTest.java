package br.lbgroup.nescharge.evcs;

import br.lbgroup.nescharge.evcs.chargingdata.ChargingData;
import br.lbgroup.nescharge.evcs.chargingdata.ChargingDataRelationsService;
import br.lbgroup.nescharge.evcs.isolated.Chargepoint;
import br.lbgroup.nescharge.evcs.isolated.ChargepointRepository;
import br.lbgroup.nescharge.evcs.isolated.OCPPServer;
import br.lbgroup.nescharge.evcs.model.Charger;
import br.lbgroup.nescharge.evcs.model.OperationMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChargerServiceTest {

    private static final String CHARGER_NAME_1 = "1";
    private static final String CHARGER_NAME_2 = "2";
    private static final String CHARGER_NAME_3 = "3";
    private static final String OWNER_1 = "Owner 1";
    private static final String OWNER_2 = "Owner 2";
    private static final String OWNER_3 = "Owner 3";

    private ChargepointRepository chargepointRepository;
    private OCPPServer ocppServer;
    private ChargingDataRelationsService chargingDataRelationsService;
    private ChargerService chargerService;

    private Charger charger;

    @BeforeEach
    void setUp() {
        chargepointRepository = mock(ChargepointRepository.class);
        ocppServer = mock(OCPPServer.class);
        chargingDataRelationsService = mock(ChargingDataRelationsService.class);
        when(chargingDataRelationsService.saveChargingData(any())).thenReturn(new ChargingData());

        charger = mock(Charger.class);

        // Mock common charger behavior
        setupChargerMocks();

        chargerService = new ChargerService(chargepointRepository, ocppServer, chargingDataRelationsService);
    }

    private void setupChargerMocks() {
        var mapMock = mock(Map.class);
        when(mapMock.get(anyString())).thenReturn("1");
        when(charger.metadata()).thenReturn(mapMock);
        when(charger.merge(any())).thenReturn(charger);
    }

    @Test
    void testFindChargerById_Success() {
        Chargepoint chargepoint = mockChargepoint();

        when(chargepointRepository.findById(1)).thenReturn(Optional.of(chargepoint));
        when(ocppServer.getChargers()).thenReturn(buildSampleOcppChargers());
        when(charger.operationMode()).thenReturn(OperationMode.AUTOMATIC_OCPP);

        Charger result = chargerService.findChargerById(1);

        assertNotNull(result);
        verifyFindChargerById();
    }

    @Test
    void testFindChargerById_ChargerNotFound() {
        when(chargepointRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> chargerService.findChargerById(1));
        verify(chargepointRepository).findById(1);
    }

    @Test
    void testStartAndStopAutomaticCharger_Success() {
        when(charger.operationMode()).thenReturn(OperationMode.AUTOMATIC_OCPP);
        when(ocppServer.startCharging(charger)).thenReturn(true);
        ChargingData expectedChargingData = new ChargingData();
        when(ocppServer.getChargingData(charger)).thenReturn(expectedChargingData);

        ChargingData startResult = chargerService.startCharging(charger);
        assertNotNull(startResult);
        verifyChargingStarted();

        ChargingData stopResult = chargerService.stopCharging(charger);
        assertEquals(expectedChargingData, stopResult);
        verify(ocppServer).stopCharging(charger);
    }

    @Test
    void testStartAndStopManualCharger_Success() {
        when(charger.operationMode()).thenReturn(OperationMode.MANUAL);

        ChargingData startResult = chargerService.startCharging(charger);
        assertNotNull(startResult);
        verify(chargingDataRelationsService).saveChargingData(any());

        ChargingData stopResult = chargerService.stopCharging(charger);
        assertNotNull(stopResult);
    }

    @Test
    void testStartCharging_AutomaticMode_Failure() {
        setupChargerOperationMode(OperationMode.AUTOMATIC_OCPP);

        // Simulate the startCharging failing
        when(ocppServer.startCharging(charger)).thenReturn(false);

        ChargingData result = chargerService.startCharging(charger);

        assertNull(result);  // ChargingData should be null if startCharging fails
        verify(ocppServer).startCharging(charger);
        verify(chargingDataRelationsService, never()).saveChargingData(any());  // Should not save data
    }

    @Test
    void testStartCharging_ManualMode_Twice_ShouldThrowIllegalStateException() {
        setupChargerOperationMode(OperationMode.MANUAL);

        // First start should succeed
        chargerService.startCharging(charger);
        verify(chargingDataRelationsService, times(1)).saveChargingData(any());

        // Simulate second start which should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> chargerService.startCharging(charger));
        verify(chargingDataRelationsService, times(1)).saveChargingData(any());  // Should not save data a second time
    }

    @Test
    void testStopCharging_AutomaticMode_Success() {
        setupChargerOperationMode(OperationMode.AUTOMATIC_OCPP);
        ChargingData expectedChargingData = new ChargingData();
        when(ocppServer.getChargingData(charger)).thenReturn(expectedChargingData);

        ChargingData result = chargerService.stopCharging(charger);

        assertEquals(expectedChargingData, result);
        verify(ocppServer).stopCharging(charger);
    }

    @Test
    void testStopCharging_ManualMode_NotCharging() {
        setupChargerOperationMode(OperationMode.MANUAL);

        assertThrows(IllegalStateException.class, () -> chargerService.stopCharging(charger));
        verify(ocppServer, never()).stopCharging(any());  // stopCharging should not be called on the server
    }

    @Test
    void testOnStopChargingAutomatically_Success() {
        setupChargerOperationMode(OperationMode.AUTOMATIC_OCPP);

        ChargingData chargingData = new ChargingData();

        Consumer<ChargingData> consumer = mock(Consumer.class);

        when(ocppServer.getChargingData(charger)).thenReturn(chargingData);

        // Mock the ocppServer to capture the callback passed to onStopChargingAutomatically
        doAnswer(invocation -> {
            // Extract the callback (Runnable) from the arguments
            Runnable callback = invocation.getArgument(1);

            // Simulate triggering the callback (i.e., stopping the charging automatically)
            callback.run();

            return null;
        }).when(ocppServer).onStopChargingAutomatically(eq(charger), any(Runnable.class));

        chargerService.onStopChargingAutomatically(charger, consumer);

        verify(ocppServer).onStopChargingAutomatically(eq(charger), any(Runnable.class));
        verify(consumer).accept(chargingData);
    }

    @Test
    void testOnStopChargingAutomatically_ShouldNotRegisterCallbackIfNotAutomatic() {
        Charger nonAutomaticCharger = new Charger(CHARGER_NAME_1, null, null, OWNER_1, OperationMode.MANUAL, Map.of());

        chargerService.onStopChargingAutomatically(nonAutomaticCharger, null);

        verify(ocppServer, never()).onStopChargingAutomatically(any(), any());
    }

    @Test
    void testStartCharging_DisabledMode_Failure() {
        setupChargerOperationMode(OperationMode.DISABLED);

        assertNull(chargerService.startCharging(charger));
        verify(ocppServer, never()).startCharging(any());
        verify(chargingDataRelationsService, never()).createRelationBetweenChargerAndChargingData(anyLong(), any());
    }

    @Test
    void testStopCharging_DisabledMode_Failure() {
        setupChargerOperationMode(OperationMode.DISABLED);

        assertNull(chargerService.stopCharging(charger));
        verify(ocppServer, never()).stopCharging(any());
    }

    @Test
    void testGetChargers_OcppServerReturnsNoChargers() {
        List<Chargepoint> databaseChargers = List.of(
                createChargepointWithLocalCharge(CHARGER_NAME_1),
                createChargepointWithLocalCharge(CHARGER_NAME_2)
        );

        // OCPP server returns no chargers
        when(ocppServer.getChargers()).thenReturn(Collections.emptyList());
        when(chargepointRepository.findAllByOperationModeNot(OperationMode.DISABLED)).thenReturn(databaseChargers);

        List<Charger> result = chargerService.getChargers();

        assertEquals(2, result.size()); // Ensure both database chargers are returned even with no OCPP chargers
        verify(chargepointRepository).findAllByOperationModeNot(OperationMode.DISABLED);
        verify(ocppServer).getChargers();
    }

    @Test
    void testGetChargers_AllOcppChargersMatch() {
        List<Chargepoint> databaseChargers = List.of(
                createChargepointWithLocalCharge(CHARGER_NAME_1),
                createChargepointWithLocalCharge(CHARGER_NAME_2)
        );

        List<Charger> ocppChargers = List.of(
                new Charger(CHARGER_NAME_1, null, null, OWNER_1, OperationMode.AUTOMATIC_OCPP, Map.of()),
                new Charger(CHARGER_NAME_2, null, null, OWNER_2, OperationMode.AUTOMATIC_OCPP, Map.of())
        );

        when(chargepointRepository.findAllByOperationModeNot(OperationMode.DISABLED)).thenReturn(databaseChargers);
        when(ocppServer.getChargers()).thenReturn(ocppChargers);

        List<Charger> result = chargerService.getChargers();

        assertEquals(2, result.size());

        assertEquals(CHARGER_NAME_1, result.get(0).name());
        assertEquals(CHARGER_NAME_2, result.get(1).name());

        verify(chargepointRepository).findAllByOperationModeNot(OperationMode.DISABLED);
        verify(ocppServer).getChargers();
    }

    @Test
    void testGetChargers_NotAllOcppChargersMatch() {
        List<Chargepoint> databaseChargers = List.of(
                createChargepointWithLocalCharge(CHARGER_NAME_1),
                createChargepointWithLocalCharge(CHARGER_NAME_2)
        );

        List<Charger> ocppChargers = List.of(
                new Charger(CHARGER_NAME_1, null, null, OWNER_1, OperationMode.AUTOMATIC_OCPP, Map.of()), // Only one matches
                new Charger(CHARGER_NAME_3, null, null, OWNER_3, OperationMode.AUTOMATIC_OCPP, Map.of())
        );

        when(chargepointRepository.findAllByOperationModeNot(OperationMode.DISABLED)).thenReturn(databaseChargers);
        when(ocppServer.getChargers()).thenReturn(ocppChargers);

        List<Charger> result = chargerService.getChargers();

        assertEquals(2, result.size());

        assertEquals(CHARGER_NAME_1, result.get(0).name());
        assertEquals(CHARGER_NAME_2, result.get(1).name());

        verify(chargepointRepository).findAllByOperationModeNot(OperationMode.DISABLED);
        verify(ocppServer).getChargers();
    }

    private void setupChargerOperationMode(OperationMode mode) {
        when(charger.operationMode()).thenReturn(mode);
    }

    private void verifyChargingStarted() {
        verify(ocppServer).startCharging(charger);
        verify(chargingDataRelationsService).createRelationBetweenChargerAndChargingData(anyLong(), any());
    }

    private void verifyFindChargerById() {
        verify(chargepointRepository).findById(1);
        verify(ocppServer).getChargers();
    }

    private Chargepoint mockChargepoint() {
        Chargepoint chargepoint = mock(Chargepoint.class);

        when(chargepoint.getLocalCharge()).thenReturn(CHARGER_NAME_1);
        when(chargepoint.toModel()).thenReturn(charger);

        return chargepoint;
    }

    private List<Charger> buildSampleOcppChargers() {
        return List.of(
                new Charger(CHARGER_NAME_1, null, null, OWNER_1, OperationMode.AUTOMATIC_OCPP, null),
                new Charger(CHARGER_NAME_2, null, null, OWNER_2, OperationMode.AUTOMATIC_OCPP, null),
                new Charger(CHARGER_NAME_3, null, null, OWNER_3, OperationMode.AUTOMATIC_OCPP, null)
        );
    }

    private Chargepoint createChargepointWithLocalCharge(String localCharge) {
        Chargepoint localChargepoint = new Chargepoint();
        localChargepoint.setLocalCharge(localCharge);
        localChargepoint.setNomeAmigavel(localCharge);
        return localChargepoint;
    }
}
