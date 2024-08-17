package br.simplipark.evcs.isolated;

import br.simplipark.evcs.OCPPServer;
import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.model.Charger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@RestController
@ConditionalOnProperty(name = "ocpp_server.mock", havingValue = "true")
public class MockOCPPServer implements OCPPServer {

    private final Map<Charger, ChargingData> chargingSessions = new HashMap<>();
    private final Map<Charger, Runnable> callbacks = new HashMap<>();

    private static final double CHARGING_RATE_WATTS_PER_SECOND = 25;

    private final OCPPServerImpl ocppServerImpl;

    public MockOCPPServer(OCPPServerImpl ocppServerImpl) {
        this.ocppServerImpl = ocppServerImpl;
    }

    @Override
    public List<Charger> getChargers() {
        return ocppServerImpl.getChargers();
    }

    @Override
    public boolean startCharging(Charger charger) {
        ChargingData chargingData = new ChargingData(0, LocalDateTime.now(), null);
        chargingSessions.put(charger, chargingData);

        log.info("Starting charging for charger [{}]", charger.name());

        return true;
    }

    @Override
    public boolean stopCharging(Charger charger) {
        ChargingData chargingData = chargingSessions.get(charger);

        if (chargingData != null) {
            log.info("Stopping charging for charger [{}]", charger.name());

            chargingData.setStoppedAt(LocalDateTime.now());

            Duration chargingDuration = Duration.between(chargingData.getStartedAt(), chargingData.getStoppedAt());
            long secondsCharged = chargingDuration.getSeconds();
            chargingData.setEnergyDeliveredInWatts(secondsCharged * CHARGING_RATE_WATTS_PER_SECOND);

            log.info("Charging stopped for charger [{}]. Energy delivered: [{}] watts.", charger.name(), chargingData.getEnergyDeliveredInWatts());

            return true;
        }

        log.warn("No active charging session found for charger [{}]", charger.name());
        return false;
    }

    @Override
    public void onStopChargingAutomatically(Charger charger, Runnable callback) {
        callbacks.put(charger, callback);
        log.info("Callback registered for charger [{}]", charger.name());
    }

    @Override
    public ChargingData getChargingData(Charger charger) {
        ChargingData chargingData = chargingSessions.getOrDefault(charger, null);
        if (chargingData != null) {
            log.info("Returning charging data for charger [{}]", charger.name());
        } else {
            log.warn("No charging data found for charger [{}]", charger.name());
        }
        return chargingData;
    }

    @GetMapping("/stop-charging/{chargerName}")
    public String manualStopCharging(@PathVariable String chargerName) {
        log.info("Received request to stop charging for charger [{}]", chargerName);

        for (var entry : callbacks.entrySet()) {
            var charger = entry.getKey();
            var runnable = entry.getValue();

            if (charger.name().equals(chargerName)) {
                log.info("Automatically stopping charging for charger [{}]", chargerName);

                stopCharging(charger);
                runnable.run();

                removeChargerFromMemory(charger);

                log.info("Charging stopped and charger [{}] removed from memory", chargerName);
                return "Charging stopped for charger " + chargerName;
            }
        }

        log.warn("Charger [{}] not found", chargerName);
        return "Charger not found";
    }

    private void removeChargerFromMemory(Charger charger) {
        chargingSessions.remove(charger);
        callbacks.remove(charger);
        log.info("Removed charger [{}] from memory", charger.name());
    }
}
