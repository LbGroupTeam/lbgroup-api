package br.lbgroup.nescharge.evcs.isolated;

import br.lbgroup.nescharge.evcs.model.OperationMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargepointRepository extends JpaRepository<Chargepoint, Integer> {
    List<Chargepoint> findAllByOperationModeNot(OperationMode operationMode);
}
