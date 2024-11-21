package br.simplipark.monitoring.view;

import br.simplipark.payment.model.PaymentStatus;
import br.simplipark.util.FormatingUtils;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ChargingDataFormattedView {
    private Long id;
    private Long transactionId;
    private Double energyDeliveredInWatts;
    private String startedAt;
    private String stoppedAt;
    private String amountBrl;
    private PaymentStatus status;
    private Long cpfUsu;
    private String nomeUsu;
    private String localCharge;
    private String ownerCharger;

    private String highlightColor;

    public ChargingDataFormattedView(Long id, Long transactionId, Double energyDeliveredInWatts, String startedAt, String stoppedAt, String amountBrl, PaymentStatus status, Long cpfUsu, String nomeUsu, String localCharge, String ownerCharger) {
        this.id = id;
        this.transactionId = transactionId;
        this.energyDeliveredInWatts = energyDeliveredInWatts;
        this.startedAt = startedAt;
        this.stoppedAt = stoppedAt;
        this.amountBrl = amountBrl;
        this.status = status;
        this.cpfUsu = cpfUsu;
        this.nomeUsu = nomeUsu;
        this.localCharge = localCharge;
        this.ownerCharger = ownerCharger;

        this.highlightColor = getHightlightColor();
    }

    public static ChargingDataFormattedView fromChargingDataView(ChargingDataView dataView) {
        String amountBrl = dataView.amountBrl() != null ? FormatingUtils.roundToTwoDecimals(dataView.amountBrl()) : null;

        String datePattern = "dd/MM/yyyy HH:mm:ss";

        return new ChargingDataFormattedView(dataView.id(), dataView.transactionId(), dataView.energyDeliveredInWatts(), FormatingUtils.formatDate(dataView.startedAt(), datePattern), FormatingUtils.formatDate(dataView.stoppedAt(), datePattern), amountBrl, dataView.status(), dataView.cpfUsu(), dataView.nomeUsu(), dataView.localCharge(), dataView.ownerCharger());
    }

    private String getHightlightColor() {
        if (cpfUsu == null) {
            return "red";
        }

        if (this.status == PaymentStatus.PENDING || this.energyDeliveredInWatts == 0.0) {
            return "yellow";
        }

        return "green";
    }
}