package br.simplipark.evcs.isolated;

import static br.simplipark.test.OCPPServerTestHelper.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.evcs.isolated.chargingdata.OCPPTransactionChargingDataRelationRepository;
import br.simplipark.evcs.model.Charger;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootTest
@Transactional
@WireMockTest(extensionScanningEnabled = true)
class OCPPServerTest {

    private static final String IDENTITY = "2024004";
    private static final String ID_TAG = "192.168.0.1";

    private static final long CHARGER_STOPPED_CHECK_INTERVAL = 50;
    private static final long TIME_TO_WAIT_FOR_CALLBACK_EXECUTION = CHARGER_STOPPED_CHECK_INTERVAL + 30;
    private static final long TIME_TO_WAIT_WHEN_STOPPING_CHARGE = 0;

    private OCPPServer ocppServer;

    @Autowired
    private ChargingDataRelationsService chargingDataRelationsService;

    @Autowired
    private OCPPTransactionChargingDataRelationRepository ocppTransactionChargingDataRelationRepository;

    @BeforeEach
    void setup(WireMockRuntimeInfo runtimeInfo) {
        ocppServer = new OCPPServer(runtimeInfo.getHttpBaseUrl(), CHARGER_STOPPED_CHECK_INTERVAL, TIME_TO_WAIT_WHEN_STOPPING_CHARGE, chargingDataRelationsService, ocppTransactionChargingDataRelationRepository);
    }

    @Nested
    class ChargersList {
        @Test
        void shouldReturnChargersList() {
            stubCentralSystemChargepointListRequest(CHARGEPOINT_LIST_JSON_BODY);

            List<Charger> chargers = ocppServer.getChargers();

            assertNotNull(chargers);
            assertEquals(18, chargers.size());
            assertEquals("20240004", chargers.getLast().name());

            verify(getRequestedFor(urlEqualTo(OCPPServerEndpoints.CHARGEPOINT_LIST.buildUrl())));
        }

        @Test
        void shouldHandleEmptyChargerList() {
            stubCentralSystemChargepointListRequest("{\"ChargePointList\": []}");

            List<Charger> chargers = ocppServer.getChargers();

            assertNotNull(chargers);
            assertTrue(chargers.isEmpty());

            verify(getRequestedFor(urlEqualTo(OCPPServerEndpoints.CHARGEPOINT_LIST.buildUrl())));
        }

        @Test
        void shouldReturnEmptyListOnInternalServerError() {
            stubInternalServerErrorRequest(OCPPServerEndpoints.CHARGEPOINT_LIST);

            List<Charger> chargers = ocppServer.getChargers();

            assertNotNull(chargers);
            assertTrue(chargers.isEmpty());

            verify(getRequestedFor(urlEqualTo(OCPPServerEndpoints.CHARGEPOINT_LIST.buildUrl())));
        }
    }

    @Nested
    class StartCharging {
        @Test
        void shouldStartChargingSuccessfully() {
            stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, ACCEPTED_JSON_RESPONSE);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertTrue(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleFailedStartCharging() {
            stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, REJECTED_JSON_RESPONSE);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertFalse(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleInternalServerErrorOnStartCharging() {
            stubInternalServerErrorRequest(OCPPServerEndpoints.START_CHARGING, IDENTITY);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertFalse(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleIdentityNotFoundOnStartCharging() {
            stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, CHARGEPOINT_NOT_FOUND_JSON_BODY);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertFalse(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleTryingToStartChargerWithNoMetadata() {
            Charger charger = createSampleChargerWithNoMetadata();

            boolean result = ocppServer.startCharging(charger);

            assertFalse(result);
        }
    }

    @Nested
    class StopCharging {
        @Test
        void shouldStopChargingSuccessfully() {
            stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, ACCEPTED_JSON_RESPONSE);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertTrue(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldReturnTrueIfAttemptingToStopChargerThatsAlreadyStopped() {
            stubCentralSystemTransactionListRequest(STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertTrue(result);

            verify(0, postRequestedFor(urlEqualTo(OCPPServerEndpoints.STOP_CHARGING.buildUrl(IDENTITY)))
                    .withRequestBody(containing("transactionId=")));
        }

        @Test
        void shouldHandleFailedStopCharging() {
            stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, REJECTED_JSON_RESPONSE);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertFalse(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleInternalServerErrorOnStopCharging() {
            stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);
            stubInternalServerErrorRequest(OCPPServerEndpoints.STOP_CHARGING, IDENTITY);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertFalse(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleIdentityNotFoundOnStopCharging() {
            stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, CHARGEPOINT_NOT_FOUND_JSON_BODY);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertFalse(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleTryingToStopChargerWithNoMetadata() {
            Charger charger = createSampleChargerWithNoMetadata();

            boolean result = ocppServer.stopCharging(charger);

            assertFalse(result);
        }
    }

    @Nested
    class AutomaticCallback {
        @Test
        void shouldTriggerStopChargingCallbackAutomaticallyButNotTwice() throws InterruptedException {
            stubCentralSystemTransactionListRequest(STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY);

            Charger charger = createSampleCharger();

            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            ocppServer.onStopChargingAutomatically(charger, () -> callbackExecuted.set(true));

            Thread.sleep(TIME_TO_WAIT_FOR_CALLBACK_EXECUTION);


            assertTrue(callbackExecuted.get(), "Callback should be executed once");

            callbackExecuted.set(false); // Reset for the next assertion.

            Thread.sleep(TIME_TO_WAIT_FOR_CALLBACK_EXECUTION);

            assertFalse(callbackExecuted.get(), "Callback should not be executed a second time");
        }

        @Test
        void shouldTriggerStopChargingCallbackAutomaticallyButNotTwiceOnExceptionAndShouldntPreventOthersFromExecuting() throws InterruptedException {
            stubCentralSystemTransactionListRequest(STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY);

            Charger chargerOne = createSampleCharger("1");
            Charger chargerTwo = createSampleCharger("2");

            AtomicBoolean callbackOneExecuted = new AtomicBoolean(false);
            ocppServer.onStopChargingAutomatically(chargerOne, () -> {
                callbackOneExecuted.set(true);

                throw new RuntimeException("Test exception");
            });

            AtomicBoolean callbackTwoExecuted = new AtomicBoolean(false);
            ocppServer.onStopChargingAutomatically(chargerTwo, () -> callbackTwoExecuted.set(true));

            Thread.sleep(TIME_TO_WAIT_FOR_CALLBACK_EXECUTION);

            assertTrue(callbackOneExecuted.get(), "Callback should be executed once");
            assertTrue(callbackTwoExecuted.get(), "Callback should be executed anyway");

            callbackOneExecuted.set(false); // Reset for the next assertion.

            Thread.sleep(TIME_TO_WAIT_FOR_CALLBACK_EXECUTION);

            assertFalse(callbackOneExecuted.get(), "Callback should not be executed a second time even if an exception is thrown");
        }

        @Test
        void shouldNotTriggerCallbackAfterStoppingManually() throws InterruptedException {
            stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, ACCEPTED_JSON_RESPONSE);

            Charger charger = createSampleCharger();

            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            ocppServer.onStopChargingAutomatically(charger, () -> callbackExecuted.set(true));

            boolean manualStopResult = ocppServer.stopCharging(charger);
            assertTrue(manualStopResult, "Manual stop should return true");

            stubCentralSystemTransactionListRequest(STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY);

            Thread.sleep(TIME_TO_WAIT_FOR_CALLBACK_EXECUTION);

            assertFalse(callbackExecuted.get(), "Callback should not be executed after manual stop");
        }

        @Test
        void shouldTriggerCallbackAfterAttemptingToStopManuallyButFails() throws InterruptedException {
            stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, REJECTED_JSON_RESPONSE);

            Charger charger = createSampleCharger();

            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            ocppServer.onStopChargingAutomatically(charger, () -> callbackExecuted.set(true));

            boolean manualStopResult = ocppServer.stopCharging(charger);
            assertFalse(manualStopResult, "Manual stop should return false on failure");

            stubCentralSystemTransactionListRequest(STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY);

            Thread.sleep(TIME_TO_WAIT_FOR_CALLBACK_EXECUTION);

            assertTrue(callbackExecuted.get(), "Callback should be executed after manual stop fails");
        }
    }

    @Test
    void shouldFetchChargingData() {
        stubCentralSystemTransactionListRequest(STILL_CHARGING_TRANSACTION_LIST_JSON_BODY);

        Charger charger = createSampleCharger();
        ChargingData chargingData = ocppServer.getChargingData(charger);

        assertNotNull(chargingData);

        assertEquals(0, chargingData.getEnergyDeliveredInWatts());
        assertEquals(chargingData.getStartedAt(), LocalDateTime.parse("2024-10-09T11:03:58"));
        assertEquals(chargingData.getStoppedAt(), LocalDateTime.parse("1899-12-30T00:00:00"));

        verify(postRequestedFor(urlEqualTo(OCPPServerEndpoints.TRANSACTION_LIST.buildUrl()))
                .withRequestBody(containing("identity=" + IDENTITY)));
    }

    private static void verifyStartChargingRequestWasMadeCorrectly() {
        verify(postRequestedFor(urlEqualTo(OCPPServerEndpoints.START_CHARGING.buildUrl(IDENTITY)))
                .withRequestBody(containing("connectorId=1"))
                .withRequestBody(containing("idTag=" + ID_TAG)));
    }

    private static void verifyStopChargingRequestWasMadeCorrectly() {
        verify(postRequestedFor(urlEqualTo(OCPPServerEndpoints.STOP_CHARGING.buildUrl(IDENTITY)))
                .withRequestBody(containing("transactionId=")));
    }

    private void stubChargepointRequest(OCPPServerEndpoints endpoint, String body) {
        stubRequestWithJsonBody(endpoint, body, IDENTITY);
    }

    private void stubCentralSystemChargepointListRequest(String jsonBody) {
        stubRequestWithJsonBody(OCPPServerEndpoints.CHARGEPOINT_LIST, jsonBody);
    }

    private void stubCentralSystemTransactionListRequest(String jsonBody) {
        stubRequestWithJsonBody(OCPPServerEndpoints.TRANSACTION_LIST, jsonBody);
    }

    private void stubRequestWithJsonBody(OCPPServerEndpoints endpoints, String jsonBody, String... params) {
        stubFor(request(endpoints.getMethod().name(), urlEqualTo(endpoints.buildUrl(params)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonBody)));
    }

    private void stubInternalServerErrorRequest(OCPPServerEndpoints endpoints, String... params) {
        stubFor(request(endpoints.getMethod().name(), urlEqualTo(endpoints.buildUrl(params)))
                .willReturn(aResponse().withStatus(500)));
    }

    private Charger createSampleCharger() {
        return createSampleCharger(IDENTITY);
    }

    private Charger createSampleCharger(String identity) {
        OCPPCharger ocppCharger = new OCPPCharger(identity, "TEST", new Connection("", OCPPServerTest.ID_TAG, ""));

        return ocppCharger.toModel();
    }

    private Charger createSampleChargerWithNoMetadata() {
        Charger charger = createSampleCharger();

        charger.metadata().clear();

        return charger;
    }
}
