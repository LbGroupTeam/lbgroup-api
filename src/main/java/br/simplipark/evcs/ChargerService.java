package br.simplipark.evcs;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.evcs.isolated.Chargepoint;
import br.simplipark.evcs.isolated.ChargepointRepository;
import br.simplipark.evcs.model.Charger;
import br.simplipark.evcs.model.OperationMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
public class ChargerService {

    private final ChargepointRepository chargepointRepository;
    private final Map<Charger, LocalDateTime> offlineChargers = new HashMap<>();
    private final OCPPServer ocppServer;
    private final ChargingDataRelationsService chargingDataRelationsService;

    private static final double WATTS_PER_HOUR_OFFLINE_CHARGER = 3500;

    public ChargerService(ChargepointRepository chargepointRepository, OCPPServer ocppServer, ChargingDataRelationsService chargingDataRelationsService) {
        this.chargepointRepository = chargepointRepository;
        this.ocppServer = ocppServer;
        this.chargingDataRelationsService = chargingDataRelationsService;
    }

    public Charger findChargerById(long id) {
        log.info("Looking for charger with ID: {}", id);
        Chargepoint chargepoint = chargepointRepository.findById((int) id).orElseThrow(() -> {
            log.error("Charger with ID {} not found in the repository.", id);
            return new NoSuchElementException("Charger not found");
        });
        var ocppChargers = ocppServer.getChargers();

        return mergeDatabaseChargerWithOcppCharger(chargepoint, ocppChargers);
    }

    public List<Charger> getChargers() {
        log.info("Fetching all chargers from the database and OCPP server");
        List<Chargepoint> databaseChargers = chargepointRepository.findAllByOperationModeNot(OperationMode.DISABLED);
        List<Charger> ocppChargers = ocppServer.getChargers();

        List<Charger> chargers = new ArrayList<>();

        for (Chargepoint databaseCharger : databaseChargers) {
            Charger charger = mergeDatabaseChargerWithOcppCharger(databaseCharger, ocppChargers);
            chargers.add(charger);
        }

        log.info("Total chargers found: {}", chargers.size());
        return chargers;
    }

    public ChargingData startCharging(Charger charger) {
        log.info("Starting charging process for charger: {}", charger.name());
        var chargingData = handleStartChargingRequest(charger);

        if (chargingData == null) {
            log.warn("Charging failed for charger: {}", charger.name());
            return null;
        }

        var chargepointId = Chargepoint.parseChargepointId(charger);
        chargingDataRelationsService.createRelationBetweenChargerAndChargingData(chargepointId, chargingData);

        log.info("Charging started successfully for charger: {}", charger.name());
        return chargingData;
    }

    public ChargingData stopCharging(Charger charger) {
        log.info("Stopping charging process for charger: {}", charger.name());
        if (charger.operationMode() == OperationMode.AUTOMATIC_OCPP) {
            ocppServer.stopCharging(charger);
            return ocppServer.getChargingData(charger);
        } else {
            return handleOfflineChargerStopChargingRequest(charger);
        }
    }

    public void onStopChargingAutomatically(Charger charger, Consumer<ChargingData> consumer) {
        log.info("Registering stop charging callback for charger: {}", charger.name());
        if (charger.operationMode() != OperationMode.AUTOMATIC_OCPP) {
            return;
        }

        Runnable callback = () -> {
            var chargingData = ocppServer.getChargingData(charger);
            log.info("Callback executed for charger: {}", charger.name());
            consumer.accept(chargingData);
        };

        ocppServer.onStopChargingAutomatically(charger, callback);
    }

    private ChargingData handleStartChargingRequest(Charger charger) {
        if (charger.operationMode() == OperationMode.AUTOMATIC_OCPP) {
            log.info("Attempting to start charging for charger in AUTOMATIC_OCPP mode: {}", charger.name());
            boolean startedSuccessfully = ocppServer.startCharging(charger);

            if (!startedSuccessfully) {
                log.warn("Failed to start charging for charger: {}", charger.name());
                return null;
            }

            log.info("Charging started successfully for charger: {}", charger.name());
            return ocppServer.getChargingData(charger);
        } else {
            return handleOfflineChargerStartChargingRequest(charger);
        }
    }

    private static Charger mergeDatabaseChargerWithOcppCharger(Chargepoint databaseCharger, List<Charger> ocppChargers) {
        Charger charger = databaseCharger.toModel();

        for (Charger ocppCharger : ocppChargers) {
            if (ocppCharger.name().equals(String.valueOf(databaseCharger.getLocalCharge()))) {
                log.info("Merging OCPP charger data with database charger data for charger: {}", charger.name());
                charger = charger.merge(ocppCharger);
                break;
            }
        }
        return charger;
    }

    private ChargingData handleOfflineChargerStartChargingRequest(Charger charger) {
        log.info("Starting charging process for offline charger: {}", charger.name());
        if (offlineChargers.containsKey(charger)) {
            log.error("Offline charger {} is already charging", charger.name());
            throw new IllegalStateException("Charger is already charging");
        }

        var startedAt = LocalDateTime.now();
        offlineChargers.put(charger, startedAt);

        var chargingData = new ChargingData(0, startedAt, null);
        chargingData = chargingDataRelationsService.saveChargingData(chargingData);

        log.info("Charging started for offline charger: {}", charger.name());
        return chargingData;
    }

    private ChargingData handleOfflineChargerStopChargingRequest(Charger charger) {
        log.info("Stopping charging process for offline charger: {}", charger.name());
        if (!offlineChargers.containsKey(charger)) {
            log.error("Offline charger {} is not charging", charger.name());
            throw new IllegalStateException("Charger is not charging");
        }

        var startedAt = offlineChargers.remove(charger);
        var stoppedAt = LocalDateTime.now();
        var duration = Duration.between(startedAt, stoppedAt);
        var hoursCharged = duration.toMinutes() / 60.0;

        log.info("Charging stopped for offline charger: {}. Duration: {} hours", charger.name(), hoursCharged);
        return new ChargingData(hoursCharged * WATTS_PER_HOUR_OFFLINE_CHARGER, startedAt, stoppedAt);
    }
}