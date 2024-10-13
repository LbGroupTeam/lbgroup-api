package br.simplipark.user.reporting;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.payment.PaymentService;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.test.TestUtils;
import br.simplipark.user.User;
import br.simplipark.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static br.simplipark.test.TestUtils.createSampleUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportingServiceTest {

    private UserService userService;
    private PaymentService paymentService;
    private ChargingDataRelationsService chargingDataRelationsService;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        paymentService = mock(PaymentService.class);
        chargingDataRelationsService = mock(ChargingDataRelationsService.class);
        reportingService = new ReportingService(userService, paymentService, chargingDataRelationsService);
    }

    @Test
    void testGenerateReportForUser_withCurrentMonthChargingData() {
        User user = createSampleUser();
        when(userService.getUserName(user)).thenReturn("testUser");
        
        List<ChargingData> chargingDataList = List.of(
                TestUtils.createSampleChargingData(1, LocalDateTime.now().minusHours(2), LocalDateTime.now()),
                TestUtils.createSampleChargingData(2, LocalDateTime.now().minusHours(1), LocalDateTime.now()),
                TestUtils.createSampleChargingData(3, LocalDateTime.now().minusHours(3), LocalDateTime.now()),
                TestUtils.createSampleChargingData(4, LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusMonths(1)),
                TestUtils.createSampleChargingData(5, LocalDateTime.now().minusMonths(2), LocalDateTime.now().minusMonths(2))
        );
        
        when(chargingDataRelationsService.findChargingDataByUserId(user.id()))
                .thenReturn(chargingDataList);

        when(paymentService.fetchPaymentsByReasonIdIn(eq(PaymentReason.EV_CHARGE), any()))
                .thenReturn(List.of(
                        createSamplePayment(100.0, "1"),
                        createSamplePayment(150.0, "2")
                ));

        ReportData reportData = reportingService.generateReportForUser(user).orElseThrow();

        assertNotNull(reportData);
        assertEquals("testUser", reportData.username());
        assertEquals(chargingDataList.size() - 2, reportData.historicalChargingData().size());

        assertTrue(reportData.historicalChargingData().get(0).startedAt().isBefore(reportData.historicalChargingData().get(1).startedAt()));

        assertEquals(0.0, reportData.historicalChargingData().getFirst().costInBrl());
        assertEquals(100.0, reportData.historicalChargingData().get(1).costInBrl());
        assertEquals(150.0, reportData.historicalChargingData().get(2).costInBrl());
    }

    @Test
    void generateReportForUser_withNoCurrentMonthChargingData() {
        User user = createSampleUser();
        when(userService.getUserName(user)).thenReturn("testUser");

        List<ChargingData> chargingDataList = List.of(
                TestUtils.createSampleChargingData(4, LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusMonths(1)),
                TestUtils.createSampleChargingData(5, LocalDateTime.now().minusMonths(2), LocalDateTime.now().minusMonths(2))
        );

        when(chargingDataRelationsService.findChargingDataByUserId(user.id()))
                .thenReturn(chargingDataList);

        ReportData reportData = reportingService.generateReportForUser(user).orElse(null);

        assertNull(reportData);
    }

    private static Payment createSamplePayment(double amount, String reasonData) {
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setReasonData(reasonData);
        return payment;
    }
}