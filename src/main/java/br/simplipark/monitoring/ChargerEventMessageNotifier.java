package br.simplipark.monitoring;

import br.simplipark.chatbot.messagedispatcher.MessageDispatcher;
import br.simplipark.chatbot.nodes.payment.PaymentInfoBuilder;
import br.simplipark.evcs.UserChargerService;
import br.simplipark.evcs.model.ChargeEvent;
import br.simplipark.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChargerEventMessageNotifier {
    private final MessageDispatcher messageDispatcher;

    private final String numbersToNotify;

    public ChargerEventMessageNotifier(MessageDispatcher messageDispatcher, UserChargerService userChargerService, @Value("${charger_service.numbers_to_notify}") String numbersToNotify) {
        this.messageDispatcher = messageDispatcher;

        this.numbersToNotify = numbersToNotify;

        if (numbersToNotify.isEmpty()) {
            log.warn("No numbers to notify were provided");
            return;
        }

        userChargerService.addListener(this::onChargerEvent);
    }

    private void onChargerEvent(ChargeEvent chargeEvent) {
        log.info("Received charger event: {}", chargeEvent);

        String templateName = "evento_carga";
        var args = buildMessageArgs(chargeEvent);

        log.debug("Sending message to numbers: {}", numbersToNotify);

        for (String number : numbersToNotify.split("\\D+")) {
            messageDispatcher.sendTemplateMessage(number, templateName, args);
        }

        log.debug("Message sent to numbers: {}", numbersToNotify);
    }

    private List<String> buildMessageArgs(ChargeEvent chargeEvent) {
        String keyword = switch (chargeEvent.type()) {
            case STARTED -> "iniciada";
            case STOPPED -> "finalizada";
        };

        User user = chargeEvent.user();

        var args = new ArrayList<>(List.of(
                keyword,
                user.name() + " (" + chargeEvent.user().cpf() + ")",
                chargeEvent.charger().toHumanString()
        ));

        args.addAll(PaymentInfoBuilder.buildChargingDataPaymentInfoArgs(chargeEvent.chargingData(), chargeEvent.amountDue()));

        return args;
    }
}
