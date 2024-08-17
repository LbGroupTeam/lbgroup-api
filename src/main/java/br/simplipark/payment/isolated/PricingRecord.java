package br.simplipark.payment.isolated;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pricescharge")
public class PricingRecord {

    @Id
    private int idPrices;

    private String typeClientPrices;
    private String ownerPrices;
    private float multiplicatorPrices;
}
