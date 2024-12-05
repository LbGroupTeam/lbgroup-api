package br.lbgroup.nescharge.evcs.isolated;

import br.lbgroup.nescharge.evcs.chargingdata.ChargingData;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record TransactionData(
        long transactionId,
        String identity,
        int connectorId,
        String idTag,
        LocalDateTime startDateTime,
        long startValue,
        LocalDateTime stopDateTime,
        long stopValue
) {
    public static TransactionData fromJson(JsonNode transactionJson) {
        var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return new TransactionData(
                transactionJson.get("TransactionId").asLong(),
                transactionJson.get("Identity").asText(),
                transactionJson.get("ConnectorId").asInt(),
                transactionJson.get("IdTag").asText(),
                LocalDateTime.parse(transactionJson.get("StartDate/Time").asText(), dateTimeFormatter).minusHours(3),
                transactionJson.get("StartValue").asLong(),
                LocalDateTime.parse(transactionJson.get("StopDate/Time").asText(), dateTimeFormatter).minusHours(3),
                transactionJson.get("StopValue").asLong()
        );
    }

    public ChargingData toChargingData() {
        return new ChargingData(
                Math.clamp(stopValue - startValue, 0, Integer.MAX_VALUE),
                startDateTime,
                stopDateTime
        );
    }
}