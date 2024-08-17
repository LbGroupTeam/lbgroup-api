package br.simplipark.payment.exceptions;

public class UnsuficientBalanceException extends Exception {
    public UnsuficientBalanceException(String message) {
        super(message);
    }
}
