package br.lbgroup.nescharge.evcs.model;

public record ChargingStation(String id, String name, String address) {
    @Override
    public String toString() {
        return name + " - " + address;
    }
}