package br.simplipark.evcs.isolated.chargingdata;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class OCPPTransactionChargingDataRelation {

    @Id
    private long chargingDataId;

    private long transactionId;
}
