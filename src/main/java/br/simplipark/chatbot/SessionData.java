package br.simplipark.chatbot;

import br.simplipark.evcs.model.Charger;
import br.simplipark.evcs.model.ChargingStation;
import br.simplipark.payment.model.PaymentMethod;
import br.simplipark.user.User;
import lombok.Data;

@Data
public class SessionData {
    private User user;

    private ChargingStation selectedChargingStation;
    private Charger selectedCharger;

    private PaymentMethod selectedPaymentMethod;

    private int lbCoinsToPurchase;
}
