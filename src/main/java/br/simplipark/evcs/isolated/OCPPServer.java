package br.simplipark.evcs.isolated;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.evcs.isolated.chargingdata.OCPPTransactionChargingDataRelation;
import br.simplipark.evcs.isolated.chargingdata.OCPPTransactionChargingDataRelationRepository;
import br.simplipark.evcs.model.Charger;
import br.simplipark.util.ThreadManager;
import br.simplipark.util.Util;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OCPPServer {

    private final String baseUrl;
    private final long chargerStoppedCheckIntervalInSeconds;

    private final Map<Charger, Runnable> callbacks = new HashMap<>();

    private final ChargingDataRelationsService chargingDataRelationsService;
    private final OCPPTransactionChargingDataRelationRepository ocppTransactionChargingDataRelationRepository;

    public OCPPServer(@Value("${ocpp.base_url}") String baseUrl, @Value("${ocpp.fetch_interval_in_seconds:60}") long chargerStoppedCheckIntervalInSeconds, ChargingDataRelationsService chargingDataRelationsService, OCPPTransactionChargingDataRelationRepository ocppTransactionChargingDataRelationRepository) {
        this.baseUrl = baseUrl;
        this.chargerStoppedCheckIntervalInSeconds = chargerStoppedCheckIntervalInSeconds;

        this.chargingDataRelationsService = chargingDataRelationsService;
        this.ocppTransactionChargingDataRelationRepository = ocppTransactionChargingDataRelationRepository;
    }

    public List<Charger> getChargers() {
        log.info("Fetching list of chargers from OCPP server...");

        var request = HttpRequest.newBuilder(URI.create(baseUrl + OCPPServerEndpoints.CHARGEPOINT_LIST.buildUrl()))
                .GET()
                .build();

        log.debug("Sending request to OCPP server: {}", request);

        List<Charger> chargers = new ArrayList<>();

        try {
            var response = Util.sendSimpleHttpRequest(request);

            var body = response.body();

            log.debug("Response body received: {}", body);

            ObjectMapper objectMapper = JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();

            var jsonNode = objectMapper.readTree(body);
            var chargePointList = jsonNode.get("ChargePointList");

            for (OCPPCharger charger : objectMapper.readValue(chargePointList.toString(), OCPPCharger[].class)) {
                chargers.add(charger.toModel());
            }

            log.info("Successfully retrieved {} chargers.", chargers.size());
        } catch (Exception e) {
            log.error("Failed to retrieve chargers", e);
        }

        return chargers;
    }

    public boolean startCharging(Charger charger) {
        try {
            var chargerIdAndTagId = parseIdAndTagIdFromCharger(charger);
            var chargerId = chargerIdAndTagId[0];
            var idTag = chargerIdAndTagId[1];

            var request = HttpRequest.newBuilder(URI.create(baseUrl + OCPPServerEndpoints.START_CHARGING.buildUrl(chargerId)))
                    .POST(HttpRequest.BodyPublishers.ofString("connectorId=1&idTag=" + idTag))
                    .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .build();

            log.debug("Sending start charging request for charger: {} with idTag: {}. Request: {}", charger, idTag, request);
            var result = parseSimpleAcceptOrRejectRequest(request);

            log.info("Charging start request for charger {} was successful: {}", charger, result);

            waitForTransactionToBeRegisteredInCentralSystem();

            return result;
        } catch (Exception e) {
            log.error("Failed to start charging for charger: {}", charger, e);
            return false;
        }
    }

    public boolean stopCharging(Charger charger) {
        try {
            log.info("Stop charging process for charger: {}", charger);

            if (hasChargerStopped(charger)) {
                log.info("Charger {} has already stopped charging", charger);

                callbacks.remove(charger);

                return true;
            }

            var chargerId = parseChargerId(charger);
            var lastTransactionIdOfCharger = fetchLastTransactionData(chargerId).transactionId();

            var request = HttpRequest.newBuilder(URI.create(baseUrl + OCPPServerEndpoints.STOP_CHARGING.buildUrl(chargerId)))
                    .POST(HttpRequest.BodyPublishers.ofString("transactionId=" + lastTransactionIdOfCharger))
                    .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .build();

            log.debug("Sending stop charging request for charger: {} with transactionId: {}. Request: {}", charger, lastTransactionIdOfCharger, request);
            boolean hasStoppedSuccessfully = parseSimpleAcceptOrRejectRequest(request);
            log.info("Charging stop request for charger {} was successful: {}", charger, hasStoppedSuccessfully);

            if (hasStoppedSuccessfully) {
                callbacks.remove(charger);

                waitForTransactionToBeRegisteredInCentralSystem();
            }

            return hasStoppedSuccessfully;
        } catch (Exception e) {
            log.error("Failed to stop charging for charger: {}", charger, e);
            return false;
        }
    }

    public void onStopChargingAutomatically(Charger charger, Runnable callback) {
        log.info("Setting up callback for automatic charging stop for charger: {}", charger);

        callbacks.put(charger, callback);

        initializeChargingListener();
    }

    public ChargingData getChargingData(Charger charger) {
        log.info("Fetching charging data for charger: {}", charger);

        var chargerId = parseChargerId(charger);

        var transactionData = fetchLastTransactionData(chargerId);
        var chargingData = transactionData.toChargingData();

        log.debug("Charging data for charger {}: {}", charger, chargingData);

        return saveTransactionDataAndChargingDataRelation(transactionData, chargingData);
    }

    private TransactionData fetchLastTransactionData(String chargerId) {
        log.info("Fetching last transaction data for charger: {}", chargerId);

        var request = HttpRequest.newBuilder(URI.create(baseUrl + OCPPServerEndpoints.TRANSACTION_LIST.buildUrl()))
                .POST(HttpRequest.BodyPublishers.ofString("identity=" + chargerId))
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        log.debug("Sending request to fetch last transaction data for chargerId: {}. Request: {}", chargerId, request);

        try {
            var response = Util.sendSimpleHttpRequest(request);

            log.debug("Transaction data response: {}", response.body());

            var mapper = JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();
            var jsonNode = mapper.readTree(response.body());

            var transactionList = jsonNode.get("TransactionList");

            var lastTransaction = transactionList.get(transactionList.size() - 1);

            return TransactionData.fromJson(lastTransaction);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch last transaction data", e);
        }
    }

    private boolean parseSimpleAcceptOrRejectRequest(HttpRequest request) throws IOException {
        var response = Util.sendSimpleHttpRequest(request);

        log.debug("Response for simple request: {}", response.body());

        var mapper = JsonMapper.builder().build();
        var jsonNode = mapper.readTree(response.body());

        if (jsonNode.has("status")) {
            var status = jsonNode.get("status").asText();

            log.info("Request status: {}", status);

            return status.equals("Accepted");
        }

        return false;
    }

    private void initializeChargingListener() {
        log.info("Initializing charging listener");

        Runnable task = () -> {
            log.info("Checking for stopped chargers...");

            var stoppedChargers = new ArrayList<Charger>();
            for (var charger : callbacks.keySet()) {
                if (hasChargerStopped(charger)) {
                    stoppedChargers.add(charger);
                }
            }

            log.info("Stopped chargers: {}", stoppedChargers);

            for (var charger : stoppedChargers) {
                var callback = callbacks.get(charger);

                log.info("Executing callback for charger: {}", charger);

                callback.run();

                log.info("Executed callback for charger: {}", charger);

                callbacks.remove(charger);
            }

            log.info("Finished stopped chargers check");
        };

        ThreadManager.schedulePeriodicTask(task, chargerStoppedCheckIntervalInSeconds, TimeUnit.SECONDS);
    }

    private boolean hasChargerStopped(Charger charger) {
        log.info("Checking if charger {} has stopped", charger);

        var chargerId = parseChargerId(charger);
        TransactionData lastTransaction;
        try {
            lastTransaction = fetchLastTransactionData(chargerId);
        } catch (Exception e) {
            log.error("Failed to fetch last transaction data for charger: {}", charger, e);
            return false;
        }

        log.info("Finished checking if charger {} has stopped", charger);

        return lastTransaction.stopValue() != 0;
    }

    private ChargingData saveTransactionDataAndChargingDataRelation(TransactionData transactionData, ChargingData chargingData) {
        log.info("Saving transaction and charging data relation for transaction: {}", transactionData.transactionId());

        var relation = ocppTransactionChargingDataRelationRepository.findByTransactionId(transactionData.transactionId());
        if (relation != null) {
            chargingData.setId(relation.getChargingDataId());

            return chargingData;
        }

        chargingData = chargingDataRelationsService.saveChargingData(chargingData);

        log.debug("Charging data saved with ID: {}", chargingData.getId());

        relation = new OCPPTransactionChargingDataRelation();
        relation.setTransactionId(transactionData.transactionId());
        relation.setChargingDataId(chargingData.getId());

        ocppTransactionChargingDataRelationRepository.save(relation);

        return chargingData;
    }

    private String parseChargerId(Charger charger) {
        return parseIdAndTagIdFromCharger(charger)[0];
    }

    private String[] parseIdAndTagIdFromCharger(Charger charger) {
        String ocppIdentity = OCPPCharger.parseOCPPIdentity(charger);
        if (ocppIdentity == null) {
            String errorMessage = "Charger doesn't have a valid OCPP identity: " + charger + ". Charger metadata: " + charger.metadata();

            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        return ocppIdentity.split("/");
    }

    private static void waitForTransactionToBeRegisteredInCentralSystem() {
        try {
            Thread.sleep(5000); // Wait for the transaction to be registered in the central system
        } catch (InterruptedException e) {
            log.error("Thread interrupted while stopping charger", e);
            Thread.currentThread().interrupt();
        }
    }
}
