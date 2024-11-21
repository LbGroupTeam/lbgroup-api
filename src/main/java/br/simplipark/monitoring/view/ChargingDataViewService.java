package br.simplipark.monitoring.view;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChargingDataViewService {

    private final EntityManager entityManager;

    public ChargingDataViewService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ChargingDataFormattedView> getChargingDataView() {
        String viewQuery = """
                        SELECT new br.simplipark.monitoring.view.ChargingDataView(
                            cd.id, ocdr.transactionId, cd.energyDeliveredInWatts,
                            cd.startedAt, cd.stoppedAt, p.amount, p.status, u.cpfUsu, u.nomeUsu,
                            c.localCharge, c.ownerCharger)
                        from ChargingData cd
                        left join Payment p on p.reasonData = CONCAT(cd.id, '')
                        left join UserChargingDataRelation ucdr on ucdr.chargingDataId = cd.id
                        left join IsolatedUser u on u.idUsu = ucdr.userId
                        left join ChargepointChargingDataRelation ccdr on ccdr.chargingDataId = cd.id
                        left join Chargepoint c on c.idCharge = ccdr.chargepointId
                        right join OCPPTransactionChargingDataRelation ocdr on ocdr.chargingDataId = cd.id
                        where p.reason = 'EV_CHARGE' or p.reason is null
                        order by cd.startedAt desc
                """;

        var chargingDataList = entityManager.createQuery(viewQuery, ChargingDataView.class).getResultList();

        List<ChargingDataFormattedView> formattedChargingDataList = new ArrayList<>();
        for (ChargingDataView chargingData : chargingDataList) {
            formattedChargingDataList.add(ChargingDataFormattedView.fromChargingDataView(chargingData));
        }

        return formattedChargingDataList;
    }
}
