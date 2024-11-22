package br.lbgroup.nescharge.evcs.model;

import br.lbgroup.commons.util.Location;

import java.util.HashMap;
import java.util.Map;

public record Charger(String name, String position, Address address, String owner, OperationMode operationMode,
                      Location location,
                      Map<String, String> metadata) {

    public Charger(String name, String position, Address address, String owner, OperationMode operationMode) {
        this(name, position, address, owner, operationMode, null, null);
    }

    public Charger {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    public String toHumanString() {
        return name + " - " + position;
    }

    public Charger merge(Charger charger) {
        Map<String, String> mergedMetadata = new HashMap<>(this.metadata);
        mergedMetadata.putAll(charger.metadata);

        return new Charger(
                this.name != null ? this.name : charger.name,
                this.position != null ? this.position : charger.position,
                this.address != null ? this.address : charger.address,
                this.owner != null ? this.owner : charger.owner,
                this.operationMode != null ? this.operationMode : charger.operationMode,
                this.location != null ? this.location : charger.location,
                mergedMetadata
        );
    }
}