package br.lbgroup.nescharge.evcs.chargingdata.relations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class UserChargingDataRelation {

    @Id
    private long chargingDataId;

    private long userId;
}
