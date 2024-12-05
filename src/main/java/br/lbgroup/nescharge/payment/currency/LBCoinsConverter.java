package br.lbgroup.nescharge.payment.currency;

public class LBCoinsConverter {
    private static final double EXCHANGE_RATE = 9.3;

    private LBCoinsConverter() {}

    public static double convertLBCoinsToBRL(double value) {
        return value / EXCHANGE_RATE;
    }

    public static double convertBRLToLBCoins(double value) {
        return value * EXCHANGE_RATE;
    }
}
