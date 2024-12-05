package br.lbgroup.nescharge.payment.model;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    LB_COINS("LB Coins"),
    CREDIT_CARD("Cartão de crédito");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }
}
