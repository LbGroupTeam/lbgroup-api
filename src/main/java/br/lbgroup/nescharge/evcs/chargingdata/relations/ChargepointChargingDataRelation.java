package br.lbgroup.nescharge.evcs.chargingdata.relations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class ChargepointChargingDataRelation {

    @Id
    private long chargingDataId;

    private long chargepointId;
}

