package br.simplipark.payment;

import br.simplipark.payment.model.PaymentOutcome;
import br.simplipark.user.User;

import java.util.function.Consumer;

public interface PaymentGateway {
    String createLinkForPayment(User user, double amount);
    void onPaymentOutcome(User user, Consumer<PaymentOutcome> outcomeHandler);
}
