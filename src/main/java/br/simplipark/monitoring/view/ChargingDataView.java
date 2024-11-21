package br.simplipark.monitoring.view;

import br.simplipark.payment.model.PaymentStatus;

import java.time.LocalDateTime;

public record ChargingDataView(
        Long id,
        Long transactionId,
        Double energyDeliveredInWatts,
        LocalDateTime startedAt,
        LocalDateTime stoppedAt,
        Double amountBrl,
        PaymentStatus status,
        Long cpfUsu,
        String nomeUsu,
        String localCharge,
        String ownerCharger
) {
}