package br.simplipark.user.reporting;

import br.simplipark.evcs.chargingdata.ChargingData;

import java.time.LocalDateTime;


public record HistoricalChargingData(double energyDeliveredInKWh, LocalDateTime startedAt, LocalDateTime stoppedAt, double costInBrl, double costInLbCoins) {

    public static HistoricalChargingData fromChargingData(ChargingData chargingData, double costInBrl, double costInLbCoins) {
        return new HistoricalChargingData(chargingData.getEnergyDeliveredInKWh(), chargingData.getStartedAt(), chargingData.getStoppedAt(), costInBrl, costInLbCoins);
    }
}