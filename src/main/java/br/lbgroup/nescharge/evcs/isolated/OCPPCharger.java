package br.lbgroup.nescharge.evcs.isolated;

import br.lbgroup.nescharge.evcs.model.Charger;

import java.util.HashMap;
import java.util.Map;

record OCPPCharger(String identity, String address, Connection connection) {
    private static final String OCPP_IDENTITY_KEY = "ocpp.identity";

    public Charger toModel() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(OCPP_IDENTITY_KEY, identity + "/" + connection.ip());

        return new Charger(identity.toUpperCase(), null, null, null, null, metadata);
    }

    public static String parseOCPPIdentity(Charger charger) {
        return charger.metadata().get(OCPP_IDENTITY_KEY);
    }
}