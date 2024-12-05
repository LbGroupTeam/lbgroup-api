package br.lbgroup.nescharge.evcs.chargingdata.relations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserChargingDataRelationRepository extends JpaRepository<UserChargingDataRelation, Long> {
    List<UserChargingDataRelation> findAllByUserId(long userId);
}
