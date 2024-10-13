package br.simplipark.test;

import br.simplipark.evcs.isolated.OCPPServerEndpoints;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class OCPPServerTestHelper {
    public static final String ACCEPTED_JSON_RESPONSE = "{\"status\": \"Accepted\"}";
    public static final String REJECTED_JSON_RESPONSE = "{\"status\": \"Rejected\"}";

    public static final String CHARGEPOINT_LIST_JSON_BODY;
    public static final String STILL_CHARGING_TRANSACTION_LIST_JSON_BODY;
    public static final String STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY;
    public static final String CHARGEPOINT_NOT_FOUND_JSON_BODY;

    private final WireMockServer wireMockServer;

    static {
        try {
            CHARGEPOINT_LIST_JSON_BODY = TestUtils.readFileFromResources("ocpp/chargepoint_list.json");
            STILL_CHARGING_TRANSACTION_LIST_JSON_BODY = TestUtils.readFileFromResources("ocpp/transaction_list_still_charging.json");
            STOPPED_CHARGING_TRANSACTION_LIST_JSON_BODY = TestUtils.readFileFromResources("ocpp/transaction_list_stopped_charging.json");
            CHARGEPOINT_NOT_FOUND_JSON_BODY = TestUtils.readFileFromResources("ocpp/chargepoint_not_found.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public OCPPServerTestHelper(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubChargepointRequest(OCPPServerEndpoints endpoint, String body) {
        stubChargepointRequest(endpoint, body, ".*?");
    }

    public void stubChargepointRequest(OCPPServerEndpoints endpoint, String body, String identity) {
        stubRequestWithJsonBody(endpoint, body, identity);
    }

    public void stubCentralSystemChargepointListRequest(String jsonBody) {
        stubRequestWithJsonBody(OCPPServerEndpoints.CHARGEPOINT_LIST, jsonBody);
    }

    public void stubCentralSystemTransactionListRequest(String jsonBody) {
        stubRequestWithJsonBody(OCPPServerEndpoints.TRANSACTION_LIST, jsonBody);
    }

    public void stubRequestWithJsonBody(OCPPServerEndpoints endpoints, String jsonBody, String... params) {
        wireMockServer.stubFor(request(endpoints.getMethod().name(), urlMatching(endpoints.buildUrl(params)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonBody)));
    }
}
