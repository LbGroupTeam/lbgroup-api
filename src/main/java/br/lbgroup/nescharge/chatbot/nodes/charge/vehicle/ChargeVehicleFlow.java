package br.lbgroup.nescharge.chatbot.nodes.charge.vehicle;

import br.lbgroup.nescharge.chatbot.ChatbotMessage;
import br.lbgroup.nescharge.chatbot.ChatbotUser;
import br.lbgroup.nescharge.chatbot.ConversationPathManager;
import br.lbgroup.nescharge.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.lbgroup.nescharge.chatbot.nodes.MainConversationStage;
import br.lbgroup.nescharge.evcs.ChargerService;
import br.lbgroup.nescharge.evcs.UserChargerService;
import br.lbgroup.nescharge.evcs.model.Charger;
import br.lbgroup.nescharge.payment.PaymentService;
import br.lbgroup.commons.util.Util;
import org.springframework.stereotype.Service;

import java.util.List;

import static br.lbgroup.nescharge.chatbot.nodes.charge.vehicle.ChargeVehicleFlowStage.CHARGING;
import static br.lbgroup.nescharge.chatbot.nodes.charge.vehicle.ChargeVehicleFlowStage.SELECT_CHARGERS;

@Service
public class ChargeVehicleFlow {

    private final QueueMessageDispatcher messageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final UserChargerService userChargerService;
    private final ChargerService chargerService;

    private final PaymentService paymentService;

    private List<Charger> chargers;

    public ChargeVehicleFlow(QueueMessageDispatcher messageDispatcher, ConversationPathManager conversationPathManager, UserChargerService userChargerService, ChargerService chargerService, PaymentService paymentService) {
        this.messageDispatcher = messageDispatcher;
        this.conversationPathManager = conversationPathManager;

        this.userChargerService = userChargerService;
        this.chargerService = chargerService;

        this.paymentService = paymentService;
    }

    public void handleMessage(ChatbotUser user, ChatbotMessage chatbotMessage) {
        if (!conversationPathManager.hasNextNode(user)) {
            handleFlowInitialization(user);

            return;
        }

        ChargeVehicleFlowStage currentStage = ChargeVehicleFlowStage.valueOf(conversationPathManager.getNextNode(user));

        switch (currentStage) {
            case SELECT_CHARGERS -> handleChargersMenuOption(user, chatbotMessage);
            case CHARGING -> handleChargingMessage(user, chatbotMessage);
        }
    }

    private void handleFlowInitialization(ChatbotUser user) {
        if (paymentService.hasPendingPayment(user.user())) {
            onChargeStopped(user, "Você possui pagamento(s) pendente(s). Por favor, finalize o pagamento antes de iniciar uma nova carga.\n\n");

            return;
        }

        conversationPathManager.addNodeToPathImmediately(user, SELECT_CHARGERS.name());
    }

    private void handleChargersMenuOption(ChatbotUser chatbotUser, ChatbotMessage message) {
        if (message.body().isEmpty()) {
            chargers = chargerService.getChargers();

            messageDispatcher.queueMessage(chatbotUser, "Segue a lista de carregadores:\n\n" + buildChargersList(chargers) +
                    "\nQual o carregador você está conectado?");

            return;
        }

        if (Util.isNotInt(message.body())) {
            handleInvalidChargerOption(chatbotUser, message);

            return;
        }

        int chargerIndex = Integer.parseInt(message.body()) - 1;

        if (chargerIndex < 0 || chargerIndex >= chargers.size()) {
            handleInvalidChargerOption(chatbotUser, message);

            return;
        }

        Charger charger = chargers.get(chargerIndex);

        boolean startedSucessfully = userChargerService.startCharging(chatbotUser.user(), charger, () -> onStopChargingAutomatically(chatbotUser));
        if (!startedSucessfully) {
            messageDispatcher.queueMessage(chatbotUser, "Tivemos problemas para iniciar a carga no carregador " + charger.name() + ".\n" +
                    "Por favor, verifique se seu carro está conectado ao carregador corretamente. Caso esteja, contate um de nossos representantes.");

            return;
        }

        chatbotUser.sessionData().setSelectedCharger(charger);

        messageDispatcher.queueMessage(chatbotUser, "Você está conectado ao carregador " + charger.toHumanString() + ".\nSua carga está em andamento.\n" +
                "Você será avisado assim que sua carga finalizar.\n\n" +
                "Caso queira parar a carga manualmente, digite 'parar'.");

        conversationPathManager.replaceLastPathNode(chatbotUser, CHARGING.name());
    }

    private void handleInvalidChargerOption(ChatbotUser chatbotUser, ChatbotMessage message) {
        messageDispatcher.queueMessage(chatbotUser, "Carregador '" + message.body() + "' inválido. Por favor, escolha um carregador válido.\n\n");

        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, SELECT_CHARGERS.name());
    }

    private void handleChargingMessage(ChatbotUser user, ChatbotMessage chatbotMessage) {
        if ("parar".equalsIgnoreCase(chatbotMessage.body())) {
            stopUserChargeManually(user);

            return;
        }

        messageDispatcher.queueMessage(user, """
                Não entendemos sua mensagem.
                Sua carga está em andamento.
                Você será avisado assim que sua carga finalizar.
                
                Caso queira parar a carga manualmente, digite 'parar'.""");
    }

    private void stopUserChargeManually(ChatbotUser chatbotUser) {
        userChargerService.stopCharging(chatbotUser.user());
        onChargeStopped(chatbotUser, "Carga finalizada manualmente.\n\n");
    }

    private void onStopChargingAutomatically(ChatbotUser chatbotUser) {
        onChargeStopped(chatbotUser, "Sua carga foi finalizada automaticamente.\n\n");
    }

    private void onChargeStopped(ChatbotUser chatbotUser, String message) {
        messageDispatcher.queueMessage(chatbotUser, message);

        conversationPathManager.navigateToImmediately(chatbotUser, MainConversationStage.PAYMENT.name());
    }

    private String buildChargersList(List<Charger> chargers) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chargers.size(); i++) {
            sb.append(i + 1).append(" - ").append(chargers.get(i).toHumanString()).append("\n\n");
        }

        return sb.toString();
    }
}