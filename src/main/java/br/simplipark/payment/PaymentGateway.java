package br.simplipark.payment;

import br.simplipark.payment.model.CompletedPayment;
import br.simplipark.user.User;

import java.util.function.Consumer;

public interface PaymentGateway {
    String createLinkForPayment(User user, double amount);
    String createLinkForLBCoinsPurchase(User user);
    void onPaymentOutcome(User user, Consumer<CompletedPayment> outcomeHandler);
}
