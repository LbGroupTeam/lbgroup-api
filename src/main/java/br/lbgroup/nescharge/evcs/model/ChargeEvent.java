package br.lbgroup.nescharge.evcs.model;

import br.lbgroup.nescharge.evcs.chargingdata.ChargingData;
import br.lbgroup.commons.user.User;

public record ChargeEvent(User user, Charger charger, ChargingData chargingData, ChargeEventType type, double amountDue) {
    public static ChargeEvent startedCharge(User user, Charger charger, ChargingData chargingData) {
        return new ChargeEvent(user, charger, chargingData, ChargeEventType.STARTED, 0);
    }

    public static ChargeEvent stoppedCharge(User user, Charger charger, ChargingData chargingData, double amountDue) {
        return new ChargeEvent(user, charger, chargingData, ChargeEventType.STOPPED, amountDue);
    }
}
