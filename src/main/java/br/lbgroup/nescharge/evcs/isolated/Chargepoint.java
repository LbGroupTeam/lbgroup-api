package br.lbgroup.nescharge.evcs.isolated;

import br.lbgroup.commons.util.Location;
import br.lbgroup.nescharge.evcs.model.Address;
import br.lbgroup.nescharge.evcs.model.Charger;
import br.lbgroup.nescharge.evcs.model.OperationMode;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Data
@Table(name = "chargepoints")
@Entity
public class Chargepoint {
    @Id
    private int idCharge;

    private String nomeAmigavel;

    private String localCharge;
    private String ownerCharger;
    private LocalDate activationDateCharger;
    private String ruaCharger;
    private String bairroCharger;
    private String cidadeCharger;
    private int numCharger;
    private int cepCharger;
    private String complementoCharger;

    @Enumerated(EnumType.STRING)
    private OperationMode operationMode;

    private double latitude;
    private double longitude;

    private static final String CHARGEPOINT_ID_KEY = "chargepoint.id";

    public Charger toModel() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(CHARGEPOINT_ID_KEY, String.valueOf(idCharge));

        return new Charger(
                nomeAmigavel,
                complementoCharger,
                new Address(
                        ruaCharger,
                        bairroCharger,
                        cidadeCharger,
                        String.valueOf(cepCharger)
                ),
                ownerCharger,
                operationMode,
                new Location(latitude, longitude),
                metadata
        );
    }

    public static long parseChargepointId(Charger charger) {
        return Long.parseLong(charger.metadata().get(CHARGEPOINT_ID_KEY));
    }
}