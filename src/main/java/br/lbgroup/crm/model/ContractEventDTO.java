package br.lbgroup.crm.model;

import java.time.LocalDateTime;

public record ContractEventDTO(
        long id,
        String description,
        String status,
        double amount,
        LocalDateTime date
) {
    public static ContractEventDTO from(ContractEvent contractEvent) {
        return new ContractEventDTO(
                contractEvent.getId(),
                contractEvent.getDescription(),
                contractEvent.getStatus(),
                contractEvent.getAmount(),
                contractEvent.getDate()
        );
    }
}
