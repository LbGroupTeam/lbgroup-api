package br.lbgroup.nescharge.evcs.chargingdata;

import java.time.LocalDateTime;

public record ChargingDataUpdate(LocalDateTime stoppedAt, double energyDeliveredInWatts) {
}
