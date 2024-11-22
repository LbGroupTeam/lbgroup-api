package br.lbgroup.nescharge.chatbot.nodes.charge.vehicle;

public enum ChargeVehicleFlowStage {
    SELECT_CHARGERS,
    CHARGING_STATION_SELECTION,
    CHARGER_SELECTION,
    IDENTITY_VERIFICATION,
    CHARGING,
    ASK_FOR_LOCATION, STOP_CHARGING
}
