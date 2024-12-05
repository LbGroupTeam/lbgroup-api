package br.lbgroup.nescharge.payment.model;

public record CompletedPayment(double amountPurchased, PaymentOutcome outcome) {
}