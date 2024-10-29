package br.simplipark.chatbot.e2e.integrations;

import br.simplipark.evcs.isolated.OCPPServerEndpoints;
import br.simplipark.test.OCPPServerTestHelper;
import br.simplipark.test.TestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class OCPPMockServer implements BeforeEachCallback {
    private static final WireMockServer server = initializeWireMockServer();

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        server.resetAll();
        initializeStubResponses();
    }

    private static WireMockServer initializeWireMockServer() {
        log.info("Initializing WireMock OCPP server");

        var server = TestUtils.createWireMockServer();

        server.start();

        initializeApplicationProperties(server.port());

        log.info("WireMock OCPP server started on port {}", server.port());

        return server;
    }

    private static void initializeStubResponses() {
        OCPPServerTestHelper ocppServerTestHelper = new OCPPServerTestHelper(server);

        ocppServerTestHelper.stubCentralSystemTransactionListRequest(OCPPServerTestHelper.STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY);
        ocppServerTestHelper.stubCentralSystemChargepointListRequest(OCPPServerTestHelper.CHARGEPOINT_LIST_JSON_BODY);

        ocppServerTestHelper.stubChargepointRequest(OCPPServerEndpoints.START_CHARGING, OCPPServerTestHelper.ACCEPTED_JSON_RESPONSE);
        ocppServerTestHelper.stubChargepointRequest(OCPPServerEndpoints.STOP_CHARGING, OCPPServerTestHelper.ACCEPTED_JSON_RESPONSE);
    }

    private static void initializeApplicationProperties(int port) {
        String baseUrl = "http://localhost:" + port;

        log.info("Initializing properties and WireMock server with baseUrl '{}'", baseUrl);
        System.setProperty("ocpp.base_url", baseUrl);
    }
}
