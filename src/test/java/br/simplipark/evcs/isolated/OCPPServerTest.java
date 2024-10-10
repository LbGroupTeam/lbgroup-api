package br.simplipark.evcs.isolated;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.evcs.isolated.chargingdata.OCPPTransactionChargingDataRelationRepository;
import br.simplipark.evcs.model.Charger;
import br.simplipark.test.TestUtils;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootTest
@WireMockTest(extensionScanningEnabled = true)
public class OCPPServerTest {

    private static final String identity = "2024004";
    private static final String idTag = "192.168.0.1";

    private static final String acceptedJsonResponse = "{\"status\": \"Accepted\"}";
    private static final String rejectedJsonResponse = "{\"status\": \"Rejected\"}";

    private static final long chargerStoppedCheckIntervalInSeconds = 1;
    private static final long secondsToWaitForCallbackExecution = chargerStoppedCheckIntervalInSeconds * 2 + 1;

    private static final String chargepointListJsonBody;
    private static final String stillChargingTransactionListJsonBody;
    private static final String stoppedChargingTransactionListJsonBody;
    private static final String chargepointNotFoundJsonBody;

    static {
        try {
            chargepointListJsonBody = TestUtils.readFileFromResources("ocpp/chargepoint_list.json");
            stillChargingTransactionListJsonBody = TestUtils.readFileFromResources("ocpp/transaction_list_still_charging.json");
            stoppedChargingTransactionListJsonBody = TestUtils.readFileFromResources("ocpp/transaction_list_stopped_charging.json");
            chargepointNotFoundJsonBody = TestUtils.readFileFromResources("ocpp/chargepoint_not_found.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OCPPServer ocppServer;

    @Autowired
    private ChargingDataRelationsService chargingDataRelationsService;

    @Autowired
    private OCPPTransactionChargingDataRelationRepository ocppTransactionChargingDataRelationRepository;

    @BeforeEach
    void setup(WireMockRuntimeInfo runtimeInfo) {
        ocppServer = new OCPPServer(runtimeInfo.getHttpBaseUrl(), chargerStoppedCheckIntervalInSeconds, chargingDataRelationsService, ocppTransactionChargingDataRelationRepository);
    }

    @Nested
    class ChargersList {
        @Test
        void shouldReturnChargersList() {
            stubCentralSystemChargepointListRequest(chargepointListJsonBody);

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
            stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, acceptedJsonResponse);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertTrue(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleFailedStartCharging() {
            stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, rejectedJsonResponse);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertFalse(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleInternalServerErrorOnStartCharging() {
            stubInternalServerErrorRequest(OCPPServerEndpoints.START_CHARGING, identity);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.startCharging(charger);

            assertFalse(result);

            verifyStartChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleIdentityNotFoundOnStartCharging() {
            stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, chargepointNotFoundJsonBody);

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
            stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, acceptedJsonResponse);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertTrue(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldReturnTrueIfAttemptingToStopChargerThatsAlreadyStopped() {
            stubCentralSystemTransactionListRequest(stoppedChargingTransactionListJsonBody);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertTrue(result);

            verify(0, postRequestedFor(urlEqualTo(OCPPServerEndpoints.STOP_CHARGING.buildUrl(identity)))
                    .withRequestBody(containing("transactionId=")));
        }

        @Test
        void shouldHandleFailedStopCharging() {
            stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, rejectedJsonResponse);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertFalse(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleInternalServerErrorOnStopCharging() {
            stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);
            stubInternalServerErrorRequest(OCPPServerEndpoints.STOP_CHARGING, identity);

            Charger charger = createSampleCharger();

            boolean result = ocppServer.stopCharging(charger);

            assertFalse(result);

            verifyStopChargingRequestWasMadeCorrectly();
        }

        @Test
        void shouldHandleIdentityNotFoundOnStopCharging() {
            stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, chargepointNotFoundJsonBody);

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
            stubCentralSystemTransactionListRequest(stoppedChargingTransactionListJsonBody);

            Charger charger = createSampleCharger();

            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            ocppServer.onStopChargingAutomatically(charger, () -> callbackExecuted.set(true));

            TimeUnit.SECONDS.sleep(secondsToWaitForCallbackExecution);


            assertTrue(callbackExecuted.get(), "Callback should be executed once");

            callbackExecuted.set(false); // Reset for the next assertion.

            TimeUnit.SECONDS.sleep(secondsToWaitForCallbackExecution);

            assertFalse(callbackExecuted.get(), "Callback should not be executed a second time");
        }

        @Test
        void shouldNotTriggerCallbackAfterStoppingManually() throws InterruptedException {
            stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, acceptedJsonResponse);

            Charger charger = createSampleCharger();

            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            ocppServer.onStopChargingAutomatically(charger, () -> callbackExecuted.set(true));

            boolean manualStopResult = ocppServer.stopCharging(charger);
            assertTrue(manualStopResult, "Manual stop should return true");

            stubCentralSystemTransactionListRequest(stoppedChargingTransactionListJsonBody);

            TimeUnit.SECONDS.sleep(secondsToWaitForCallbackExecution);

            assertFalse(callbackExecuted.get(), "Callback should not be executed after manual stop");
        }

        @Test
        void shouldTriggerCallbackAfterAttemptingToStopManuallyButFails() throws InterruptedException {
            stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);
            stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, rejectedJsonResponse);

            Charger charger = createSampleCharger();

            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            ocppServer.onStopChargingAutomatically(charger, () -> callbackExecuted.set(true));

            boolean manualStopResult = ocppServer.stopCharging(charger);
            assertFalse(manualStopResult, "Manual stop should return false on failure");

            stubCentralSystemTransactionListRequest(stoppedChargingTransactionListJsonBody);

            TimeUnit.SECONDS.sleep(secondsToWaitForCallbackExecution);

            assertTrue(callbackExecuted.get(), "Callback should be executed after manual stop fails");
        }
    }

    @Test
    void shouldFetchChargingData() {
        stubCentralSystemTransactionListRequest(stillChargingTransactionListJsonBody);

        Charger charger = createSampleCharger();
        ChargingData chargingData = ocppServer.getChargingData(charger);

        assertNotNull(chargingData);

        assertEquals(0, chargingData.getEnergyDeliveredInWatts());
        assertEquals(chargingData.getStartedAt(), LocalDateTime.parse("2024-10-09T11:03:58"));
        assertEquals(chargingData.getStoppedAt(), LocalDateTime.parse("1899-12-30T00:00:00"));

        verify(postRequestedFor(urlEqualTo(OCPPServerEndpoints.TRANSACTION_LIST.buildUrl()))
                .withRequestBody(containing("identity=" + identity)));
    }

    private static void verifyStartChargingRequestWasMadeCorrectly() {
        verify(postRequestedFor(urlEqualTo(OCPPServerEndpoints.START_CHARGING.buildUrl(identity)))
                .withRequestBody(containing("connectorId=1"))
                .withRequestBody(containing("idTag=" + idTag)));
    }

    private static void verifyStopChargingRequestWasMadeCorrectly() {
        verify(postRequestedFor(urlEqualTo(OCPPServerEndpoints.STOP_CHARGING.buildUrl(identity)))
                .withRequestBody(containing("transactionId=")));
    }

    private void stubChargepointRequest(OCPPServerEndpoints endpoint, String body) {
        stubRequestWithJsonBody(endpoint, body, identity);
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
        OCPPCharger ocppCharger = new OCPPCharger(identity, "TEST", new Connection("", OCPPServerTest.idTag, ""));

        return ocppCharger.toModel();
    }

    private Charger createSampleChargerWithNoMetadata() {
        Charger charger = createSampleCharger();

        charger.metadata().clear();

        return charger;
    }
}
