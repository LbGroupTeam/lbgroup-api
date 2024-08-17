package br.simplipark.evcs.isolated;

import br.simplipark.evcs.chargingdata.ChargingData;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record TransactionData(
        int transactionId,
        String identity,
        int connectorId,
        String idTag,
        LocalDateTime startDateTime,
        int startValue,
        LocalDateTime stopDateTime,
        int stopValue
) {
    public static TransactionData fromJson(JsonNode transactionJson) {
        var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return new TransactionData(
                transactionJson.get("TransactionId").asInt(),
                transactionJson.get("Identity").asText(),
                transactionJson.get("ConnectorId").asInt(),
                transactionJson.get("IdTag").asText(),
                LocalDateTime.parse(transactionJson.get("StartDate/Time").asText(), dateTimeFormatter),
                transactionJson.get("StartValue").asInt(),
                LocalDateTime.parse(transactionJson.get("StopDate/Time").asText(), dateTimeFormatter),
                transactionJson.get("StopValue").asInt()
        );
    }

    public ChargingData toChargingData() {
        return new ChargingData(
                Math.clamp(stopValue - (long) startValue, 0, Integer.MAX_VALUE),
                startDateTime,
                stopDateTime
        );
    }
}