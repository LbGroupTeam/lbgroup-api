package br.simplipark.evcs;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.model.Charger;

import java.util.List;

public interface OCPPServer {
    List<Charger> getChargers();
    boolean startCharging(Charger charger);
    boolean stopCharging(Charger charger);

    void onStopChargingAutomatically(Charger charger, Runnable callback);

    ChargingData getChargingData(Charger charger);
}