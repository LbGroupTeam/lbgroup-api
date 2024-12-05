package br.lbgroup.nescharge.chatbot;

import br.lbgroup.nescharge.evcs.model.Charger;
import br.lbgroup.nescharge.evcs.model.ChargingStation;
import br.lbgroup.nescharge.payment.model.PaymentMethod;
import br.lbgroup.commons.user.User;
import lombok.Data;

@Data
public class SessionData {
    private User user;

    private ChargingStation selectedChargingStation;
    private Charger selectedCharger;

    private PaymentMethod selectedPaymentMethod;

    private int lbCoinsToPurchase;
}
