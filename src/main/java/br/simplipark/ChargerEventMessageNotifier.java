package br.simplipark;

import br.simplipark.chatbot.messagedispatcher.MessageDispatcher;
import br.simplipark.chatbot.nodes.payment.PaymentInfoBuilder;
import br.simplipark.evcs.UserChargerService;
import br.simplipark.evcs.model.ChargeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

        var message = buildMessage(chargeEvent);

        log.debug("Sending message {} to numbers: {}", message, numbersToNotify);

        for (String number : numbersToNotify.split("\\D+")) {
            messageDispatcher.sendMessage(number, message);
        }

        log.debug("Message sent to numbers: {}", numbersToNotify);
    }

    private String buildMessage(ChargeEvent chargeEvent) {
        String keyword = switch (chargeEvent.type()) {
            case STARTED -> "Carga iniciada";
            case STOPPED -> "Carga finalizada";
        };

        return keyword + " para CPF " + chargeEvent.user().cpf() + " no carregador " + chargeEvent.charger().name() + ":\n\n" +
               PaymentInfoBuilder.buildChargingDataPaymentInfo(chargeEvent.chargingData(), chargeEvent.amountDue());
    }
}
