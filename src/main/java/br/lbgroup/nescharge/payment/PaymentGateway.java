package br.lbgroup.nescharge.payment;

import br.lbgroup.nescharge.payment.model.CompletedPayment;
import br.lbgroup.commons.user.User;

import java.util.function.Consumer;

public interface PaymentGateway {
    String createLinkForPayment(User user, double amount);
    String createLinkForLBCoinsPurchase(User user);
    void onPaymentOutcome(User user, Consumer<CompletedPayment> outcomeHandler);
}
