package br.simplipark.chatbot.e2e.integrations;

import br.simplipark.evcs.isolated.OCPPServerEndpoints;
import br.simplipark.test.OCPPServerTestHelper;
import br.simplipark.test.TestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OCPPMockServer {
    static {
        initializeWireMockServer();
    }

    private static void initializeWireMockServer() {
        log.info("Initializing WireMock OCPP server");

        var server = TestUtils.createWireMockServer();

        initializeStubResponses(server);

        server.start();

        initializeApplicationProperties(server.port());

        log.info("WireMock OCPP server started on port {}", server.port());
    }

    private static void initializeStubResponses(WireMockServer server) {
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
