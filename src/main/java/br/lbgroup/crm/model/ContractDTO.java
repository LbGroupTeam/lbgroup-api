package br.lbgroup.crm.model;

import java.time.LocalDate;

public record ContractDTO(
        Long id,
        String description,
        String company,
        String product,
        Double value,
        LocalDate startDate,
        LocalDate stopDate
) {
    public static ContractDTO from(Contract contract) {
        return new ContractDTO(
                contract.getId(),
                contract.getDescription(),
                contract.getCompany(),
                contract.getProduct(),
                contract.getValue(),
                contract.getStartDate(),
                contract.getStopDate()
        );
    }
}
