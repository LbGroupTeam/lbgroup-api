package br.lbgroup.nescharge.chatbot.nodes.payment;

public enum PaymentFlowStage {
    SHOW_PAYMENT_METHODS,
    PAYMENT_METHOD_SELECTION,
    LB_COINS_PAYMENT,
    CREDIT_CARD_PAYMENT,
    PAYMENT_CONFIRMATION,
    PAYMENT_ACCEPTED,
    PAYMENT_IN_PROGRESS,
    PAYMENT_REJECTED
}
