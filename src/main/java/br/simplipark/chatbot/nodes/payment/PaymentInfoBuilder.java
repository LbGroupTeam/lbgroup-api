package br.simplipark.chatbot.nodes.payment;

import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.util.FormatingUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static br.simplipark.payment.PaymentService.calculateTotalCost;

@Service
public class PaymentInfoBuilder {

    private final ChargingDataRelationsService chargingDataRelationsService;

    public PaymentInfoBuilder(ChargingDataRelationsService chargingDataRelationsService) {
        this.chargingDataRelationsService = chargingDataRelationsService;
    }

    public String buildPaymentInfoMessage(List<Payment> pendingPayments) {
        double totalCost = calculateTotalCost(pendingPayments);

        StringBuilder sb = new StringBuilder();
        sb.append("Pagamentos pendentes:\n");

        for (Payment payment : pendingPayments) {
            sb.append(buildPaymentInfo(payment));

            sb.append("\n\n");
        }

        sb.append("Total: *R$").append(FormatingUtils.roundToTwoDecimals(totalCost)).append("*\n\n");

        return sb.toString();
    }

    private String buildPaymentInfo(Payment payment) {
        if (payment.getReason() == PaymentReason.EV_CHARGE) {
            return buildEvChargePaymentInfo(payment);
        }

        return "";
    }

    private String buildEvChargePaymentInfo(Payment payment) {
        long chargingDataId = Long.parseLong(payment.getReasonData());

        var chargingData = chargingDataRelationsService.findChargingDataById(chargingDataId);

        var formattedEnergy = FormatingUtils.roundToTwoDecimals(chargingData.getEnergyDeliveredInKWh());
        var formattedCost = FormatingUtils.roundToTwoDecimals(payment.getAmount());

        return "Kwh carregados: " + formattedEnergy + "\n" +
               "Valor total: *R$ " + formattedCost + "*\n" +
               "Data de início: " + FormatingUtils.formatDateMedium(chargingData.getStartedAt()) + "\n" +
               "Data de término: " + FormatingUtils.formatDateMedium(chargingData.getStoppedAt());
    }
}
