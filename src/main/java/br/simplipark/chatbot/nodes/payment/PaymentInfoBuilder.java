package br.simplipark.chatbot.nodes.payment;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.chargingdata.ChargingDataRelationsService;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentReason;
import br.simplipark.util.FormatingUtils;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
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

    public static String buildChargingDataPaymentInfo(ChargingData chargingData, double amountDue) {
        var args = buildChargingDataPaymentInfoArgs(chargingData, amountDue);

        return MessageFormat.format("""
                Kwh carregados: {0}
                Valor total: *R$ {1}*
                Data de início: {2}
                Data de término: {3}""", args.toArray());
    }

    public static List<String> buildChargingDataPaymentInfoArgs(ChargingData chargingData, double amountDue) {
        var formattedEnergy = FormatingUtils.roundToTwoDecimals(chargingData.getEnergyDeliveredInKWh());
        var formattedCost = FormatingUtils.roundToTwoDecimals(amountDue);

        return List.of(formattedEnergy, formattedCost, FormatingUtils.formatDateMedium(chargingData.getStartedAt()), FormatingUtils.formatDateMedium(chargingData.getStoppedAt()));
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

        return buildChargingDataPaymentInfo(chargingData, payment.getAmount());
    }
}
