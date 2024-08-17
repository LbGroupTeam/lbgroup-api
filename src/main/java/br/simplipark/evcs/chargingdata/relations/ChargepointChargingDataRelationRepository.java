package br.simplipark.evcs.chargingdata.relations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargepointChargingDataRelationRepository extends JpaRepository<ChargepointChargingDataRelation, Long> {
    ChargepointChargingDataRelation findByChargingDataId(long chargingDataId);
}
