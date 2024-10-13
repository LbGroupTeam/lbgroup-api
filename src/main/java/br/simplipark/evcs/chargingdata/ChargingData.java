package br.simplipark.evcs.chargingdata;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Entity
@AllArgsConstructor
public class ChargingData {

    @Id
    @GeneratedValue
    private Long id;

    private double energyDeliveredInWatts;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;

    public ChargingData() {
    }

    public ChargingData(double energyDeliveredInWatts, LocalDateTime startedAt, LocalDateTime stoppedAt) {
        this.energyDeliveredInWatts = energyDeliveredInWatts;
        this.startedAt = startedAt;
        this.stoppedAt = stoppedAt;
    }

    public double getEnergyDeliveredInKWh() {
        return energyDeliveredInWatts / 1000;
    }
}
