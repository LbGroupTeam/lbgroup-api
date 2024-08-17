package br.simplipark.chatbot.nodes.charge.vehicle;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.ChatbotUser;
import br.simplipark.chatbot.ConversationPathManager;
import br.simplipark.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.simplipark.chatbot.nodes.MainConversationStage;
import br.simplipark.evcs.ChargerService;
import br.simplipark.evcs.UserChargerService;
import br.simplipark.evcs.model.Charger;
import br.simplipark.payment.PaymentService;
import br.simplipark.util.Util;
import org.springframework.stereotype.Service;

import java.util.List;

import static br.simplipark.chatbot.nodes.charge.vehicle.ChargeVehicleFlowStage.CHARGING;
import static br.simplipark.chatbot.nodes.charge.vehicle.ChargeVehicleFlowStage.SELECT_CHARGERS;

@Service
public class ChargeVehicleFlow {

    private final QueueMessageDispatcher messageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final UserChargerService userChargerService;
    private final ChargerService chargerService;

    private final PaymentService paymentService;

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
            messageDispatcher.queueMessage(chatbotUser, "Segue a lista de carregadores:\n\n" + buildChargersList() +
                    "\nQual o carregador você está conectado?");

            return;
        }

        if (Util.isNotInt(message.body())) {
            handleInvalidChargerOption(chatbotUser, "Carregador '" + message.body() + "' inválido. Por favor, escolha um carregador válido.\n\n");

            return;
        }

        int chargerIndex = Integer.parseInt(message.body()) - 1;

        var chargers = chargerService.getChargers();

        if (chargerIndex < 0 || chargerIndex >= chargers.size()) {
            handleInvalidChargerOption(chatbotUser, "Carregador '" + message.body() + "' inválido. Por favor, escolha um carregador válido.\n\n");

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

        messageDispatcher.queueMessage(chatbotUser, "Você está conectado ao carregador " + charger.name() + ".\nSua carga está em andamento.\n" +
                "Você será avisado assim que sua carga finalizar.\n\n" +
                "Caso queira parar a carga manualmente, digite 'parar'.");

        conversationPathManager.replaceLastPathNode(chatbotUser, CHARGING.name());
    }

    private void handleInvalidChargerOption(ChatbotUser chatbotUser, String message) {
        messageDispatcher.queueMessage(chatbotUser, message);

        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, SELECT_CHARGERS.name());
    }

    private void handleChargingMessage(ChatbotUser user, ChatbotMessage chatbotMessage) {
        if ("parar".equalsIgnoreCase(chatbotMessage.body())) {
            stopUserChargeManually(user);

            return;
        }

        messageDispatcher.queueMessage(user, "Não entendemos sua mensagem. Sua carga está em andamento. " +
                "Você será avisado assim que sua carga finalizar. " +
                "Caso queira parar a carga manualmente, digite 'parar'.");
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

    private String buildChargersList() {
        StringBuilder sb = new StringBuilder();

        List<Charger> chargers = chargerService.getChargers();

        for (int i = 0; i < chargers.size(); i++) {
            sb.append(i + 1).append(" - ").append(chargers.get(i)).append("\n\n");
        }

        return sb.toString();
    }
}