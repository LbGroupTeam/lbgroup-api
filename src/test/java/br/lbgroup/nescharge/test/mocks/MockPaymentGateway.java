package br.lbgroup.nescharge.test.mocks;

import br.lbgroup.nescharge.payment.PaymentGateway;
import br.lbgroup.nescharge.payment.model.CompletedPayment;
import br.lbgroup.nescharge.payment.model.PaymentOutcome;
import br.lbgroup.commons.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MockPaymentGateway implements PaymentGateway {
    private final List<Consumer<CompletedPayment>> outcomeHandlers = new ArrayList<>();
    private final String basePaymentLink;

    public MockPaymentGateway(String basePaymentLink) {
        this.basePaymentLink = basePaymentLink;
    }

    @Override
    public String createLinkForPayment(User user, double amount) {
        return basePaymentLink + "?userId=" + user.id() + "&amount=" + amount;
    }

    @Override
    public void onPaymentOutcome(User user, Consumer<CompletedPayment> outcomeHandler) {
        outcomeHandlers.add(outcomeHandler);
    }

    @Override
    public String createLinkForLBCoinsPurchase(User user) {
        return basePaymentLink + "?client_reference_id=" + user.id();
    }

    public void triggerPaymentOutcomeAccepted() {
        triggerPaymentOutcome(PaymentOutcome.ACCEPTED);
    }

    public void triggerPaymentOutcomeRejected() {
        triggerPaymentOutcome(PaymentOutcome.REJECTED);
    }

    public void triggerPaymentOutcome(PaymentOutcome outcome) {
        triggerPaymentOutcome(0, outcome);
    }

    public void triggerPaymentOutcome(double amountPurchased, PaymentOutcome outcome) {
        for (Consumer<CompletedPayment> handler : outcomeHandlers) {
            handler.accept(new CompletedPayment(amountPurchased, outcome));
        }

        outcomeHandlers.clear();
    }
}