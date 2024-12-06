package br.lbgroup.nescharge.evcs.isolated;

import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMethod;

import java.text.MessageFormat;

@Getter
public enum OCPPServerEndpoints {
    CHARGEPOINT_LIST(RequestMethod.GET, "/CentralSystem/ChargePointList"),
    TRANSACTION_LIST(RequestMethod.POST, "/CentralSystem/TransactionList"),
    START_CHARGING(RequestMethod.POST, "/ChargePoint/{0}/RemoteStartTransaction"),
    STOP_CHARGING(RequestMethod.POST, "/ChargePoint/{0}/RemoteStopTransaction");

    private final String endpoint;
    private final RequestMethod method;

    OCPPServerEndpoints(RequestMethod method, String endpoint) {
        this.method = method;
        this.endpoint = endpoint;
    }

    public String buildUrl(String... params) {
        return MessageFormat.format(endpoint, (Object[]) params);
    }
}
