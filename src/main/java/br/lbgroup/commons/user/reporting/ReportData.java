package br.lbgroup.commons.user.reporting;

import java.util.List;

public record ReportData(List<HistoricalChargingData> historicalChargingData, String username) {
    public SummedUpReportData sumUp() {
        double totalEnergyDelivered = historicalChargingData.stream().mapToDouble(HistoricalChargingData::energyDeliveredInKWh).sum();
        double totalCostInBrl = historicalChargingData.stream().mapToDouble(HistoricalChargingData::costInBrl).sum();
        double totalCostInLbCoins = historicalChargingData.stream().mapToDouble(HistoricalChargingData::costInLbCoins).sum();

        return new SummedUpReportData(totalEnergyDelivered, totalCostInBrl, totalCostInLbCoins);
    }
}