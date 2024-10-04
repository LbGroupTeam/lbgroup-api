package br.simplipark.payment.model;

public record CompletedPayment(double amountPurchased, PaymentOutcome outcome) {
}