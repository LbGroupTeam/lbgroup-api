package br.simplipark.evcs.isolated.chargingdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OCPPTransactionChargingDataRelationRepository extends JpaRepository<OCPPTransactionChargingDataRelation, Long> {
    OCPPTransactionChargingDataRelation findByTransactionId(long transactionId);
}
