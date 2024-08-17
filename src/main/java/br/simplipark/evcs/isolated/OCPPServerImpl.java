package br.simplipark.evcs.isolated;

import br.simplipark.evcs.OCPPServer;
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
public class OCPPServerImpl implements OCPPServer {

    private static final String BASE_URL = "http://54.81.97.51:9220/api/v1";
    private static final String CENTRAL_SYSTEM_URL = "/CentralSystem";
    private static final String CHARGE_POINT_URL = "/ChargePoint";

    private final Map<Charger, Runnable> callbacks = new HashMap<>();

    private final ChargingDataRelationsService chargingDataRelationsService;
    private final OCPPTransactionChargingDataRelationRepository ocppTransactionChargingDataRelationRepository;

    public OCPPServerImpl(ChargingDataRelationsService chargingDataRelationsService, OCPPTransactionChargingDataRelationRepository ocppTransactionChargingDataRelationRepository) {
        this.chargingDataRelationsService = chargingDataRelationsService;
        this.ocppTransactionChargingDataRelationRepository = ocppTransactionChargingDataRelationRepository;
    }

    @Override
    public List<Charger> getChargers() {
        log.info("Fetching list of chargers from OCPP server...");

        var request = HttpRequest.newBuilder(URI.create(BASE_URL + CENTRAL_SYSTEM_URL + "/ChargePointList"))
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
        } catch (IOException e) {
            log.error("Failed to retrieve chargers", e);
            throw new RuntimeException(e);
        }

        return chargers;
    }

    @Override
    public boolean startCharging(Charger charger) {
        var chargerIdAndTagId = parseIdAndTagIdFromCharger(charger);
        var chargerId = chargerIdAndTagId[0];
        var idTag = chargerIdAndTagId[1];

        var request = HttpRequest.newBuilder(URI.create(BASE_URL + CHARGE_POINT_URL + "/" + chargerId + "/RemoteStartTransaction"))
                .POST(HttpRequest.BodyPublishers.ofString("connectorId=1&idTag=" + idTag))
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        log.debug("Sending start charging request for charger: {} with idTag: {}. Request: {}", charger, idTag, request);
        var result = parseSimpleAcceptOrRejectRequest(request);

        log.info("Charging start request for charger {} was successful: {}", charger, result);

        return result;
    }

    @Override
    public boolean stopCharging(Charger charger) {
        log.info("Starting charging process for charger: {}", charger);

        var chargerId = parseChargerId(charger);
        var lastTransactionIdOfCharger = fetchLastTransactionData(chargerId).transactionId();

        var request = HttpRequest.newBuilder(URI.create(BASE_URL + CHARGE_POINT_URL + "/" + chargerId + "/RemoteStopTransaction"))
                .POST(HttpRequest.BodyPublishers.ofString("transactionId=" + lastTransactionIdOfCharger))
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        log.debug("Sending stop charging request for charger: {} with transactionId: {}. Request: {}", charger, lastTransactionIdOfCharger, request);
        boolean hasStoppedSuccessfully = parseSimpleAcceptOrRejectRequest(request);
        log.info("Charging stop request for charger {} was successful: {}", charger, hasStoppedSuccessfully);

        if (hasStoppedSuccessfully) {
            callbacks.remove(charger);
        }

        return hasStoppedSuccessfully;
    }

    @Override
    public void onStopChargingAutomatically(Charger charger, Runnable callback) {
        log.info("Setting up callback for automatic charging stop for charger: {}", charger);

        callbacks.put(charger, callback);

        initializeChargingListener();
    }

    @Override
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

        try {
            Thread.sleep(5000); // Wait for the transaction to be registered in the central system
        } catch (InterruptedException e) {
            log.error("Thread interrupted while fetching last transaction data", e);
            Thread.currentThread().interrupt();
        }

        var request = HttpRequest.newBuilder(URI.create(BASE_URL + CENTRAL_SYSTEM_URL + "/TransactionList"))
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
            log.error("Failed to fetch last transaction data", e);
            throw new RuntimeException(e);
        }
    }

    private boolean parseSimpleAcceptOrRejectRequest(HttpRequest request) {
        try {
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

        } catch (IOException e) {
            log.error("Failed to parse request", e);
            throw new RuntimeException(e);
        }
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
                callback.run();

                log.info("Executed callback for charger: {}", charger);

                callbacks.remove(charger);
            }
        };

        ThreadManager.schedulePeriodicTask(task, 60, TimeUnit.SECONDS);
    }

    private boolean hasChargerStopped(Charger charger) {
        log.info("Checking if charger {} has stopped", charger);

        var chargerId = parseChargerId(charger);
        var lastTransaction = fetchLastTransactionData(chargerId);

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
        return OCPPCharger.parseOCPPIdentity(charger).split("/");
    }

    public static void main(String[] args) {
        OCPPServer ocppServer = new OCPPServerImpl(null, null);
        var chargers = ocppServer.getChargers();

//        System.out.println(ocppServer.startCharging(chargers.getLast()));

//        ocppServer.stopCharging(chargers.getLast());

        var chargingData = ocppServer.getChargingData(chargers.getLast());
        System.out.println(chargingData);
    }
}
