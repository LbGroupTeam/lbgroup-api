package br.lbgroup.nescharge.evcs.chargingdata;

import br.lbgroup.nescharge.evcs.chargingdata.relations.ChargepointChargingDataRelation;
import br.lbgroup.nescharge.evcs.chargingdata.relations.ChargepointChargingDataRelationRepository;
import br.lbgroup.nescharge.evcs.chargingdata.relations.UserChargingDataRelation;
import br.lbgroup.nescharge.evcs.chargingdata.relations.UserChargingDataRelationRepository;
import br.lbgroup.commons.user.User;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
public class ChargingDataRelationsService {

    private final ChargingDataRepository chargingDataRepository;
    private final ChargepointChargingDataRelationRepository chargepointChargingDataRelationRepository;
    private final UserChargingDataRelationRepository userChargingDataRelationRepository;

    public ChargingDataRelationsService(ChargingDataRepository chargingDataRepository,
                                        ChargepointChargingDataRelationRepository chargepointChargingDataRelationRepository,
                                        UserChargingDataRelationRepository userChargingDataRelationRepository) {
        this.chargingDataRepository = chargingDataRepository;
        this.chargepointChargingDataRelationRepository = chargepointChargingDataRelationRepository;
        this.userChargingDataRelationRepository = userChargingDataRelationRepository;
    }

    public ChargingData findChargingDataById(long chargingDataId) {
        log.debug("Fetching charging data with ID [{}].", chargingDataId);
        return chargingDataRepository.findById(chargingDataId)
                .orElseThrow(() -> {
                    log.error("Charging data not found for ID [{}].", chargingDataId);
                    return new RuntimeException("Charging data not found for ID: " + chargingDataId);
                });
    }

    public ChargingData saveChargingData(ChargingData chargingData) {
        log.debug("Saving charging data: [{}].", chargingData);
        ChargingData savedChargingData = chargingDataRepository.save(chargingData);
        log.debug("Charging data saved successfully with ID [{}].", savedChargingData.getId());
        return savedChargingData;
    }

    public ChargingData updateChargingDataWithNewMeasurements(long chargingDataId, ChargingData chargingDataWithUpdatedValues) {
        log.debug("Updating charging data with new measurements for ID [{}].", chargingDataId);
        var chargingData = findChargingDataById(chargingDataId);

        chargingData.setStoppedAt(chargingDataWithUpdatedValues.getStoppedAt());
        chargingData.setEnergyDeliveredInWatts(chargingDataWithUpdatedValues.getEnergyDeliveredInWatts());

        ChargingData updatedChargingData = chargingDataRepository.save(chargingData);
        log.debug("Charging data with ID [{}] updated successfully.", chargingDataId);
        return updatedChargingData;
    }

    public void createRelationBetweenChargerAndChargingData(long chargerId, ChargingData chargingData) {
        log.debug("Creating relation between charger [{}] and charging data [{}].", chargerId, chargingData.getId());
        var chargepointChargingDataRelation = new ChargepointChargingDataRelation();

        chargepointChargingDataRelation.setChargepointId(chargerId);
        chargepointChargingDataRelation.setChargingDataId(chargingData.getId());

        chargepointChargingDataRelationRepository.save(chargepointChargingDataRelation);
        log.debug("Relation created between charger [{}] and charging data [{}].", chargerId, chargingData.getId());
    }

    public void createRelationBetweenUserAndChargingData(User user, ChargingData chargingData) {
        log.debug("Creating relation between User [{}] and charging data [{}].", user.id(), chargingData.getId());
        var userChargingDataRelation = new UserChargingDataRelation();

        userChargingDataRelation.setUserId(user.id());
        userChargingDataRelation.setChargingDataId(chargingData.getId());

        userChargingDataRelationRepository.save(userChargingDataRelation);
        log.debug("Relation created between User [{}] and charging data [{}].", user.id(), chargingData.getId());
    }

    public long findChargerIdByChargingDataId(long chargingDataId) {
        log.debug("Fetching charger ID by charging data ID [{}].", chargingDataId);
        var relation = chargepointChargingDataRelationRepository.findByChargingDataId(chargingDataId);
        log.debug("Charger ID [{}] found for charging data ID [{}].", relation.getChargepointId(), chargingDataId);
        return relation.getChargepointId();
    }

    public List<ChargingData> findChargingDataByUserId(long userId) {
        log.debug("Fetching charging data by user ID [{}].", userId);
        var relations = userChargingDataRelationRepository.findAllByUserId(userId);

        var chargingDataIds = relations.stream().map(UserChargingDataRelation::getChargingDataId).toList();

        var chargingData = chargingDataRepository.findAllById(chargingDataIds);
        log.debug("Found {} total charging records for user ID: {}", chargingData.size(), userId);

        return chargingData;
    }
}
