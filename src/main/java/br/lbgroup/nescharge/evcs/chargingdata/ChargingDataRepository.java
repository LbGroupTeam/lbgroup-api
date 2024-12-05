package br.lbgroup.nescharge.evcs.chargingdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingDataRepository extends JpaRepository<ChargingData, Long> {
}
