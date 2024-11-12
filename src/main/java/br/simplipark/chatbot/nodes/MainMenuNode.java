package br.simplipark.chatbot.nodes;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.ChatbotUser;
import br.simplipark.chatbot.ConversationPathManager;
import br.simplipark.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.simplipark.chatbot.nodes.charge.ReportingFlow;
import br.simplipark.chatbot.nodes.charge.vehicle.ChargeVehicleFlow;
import br.simplipark.chatbot.nodes.lbcoin.LbCoinsPurchaseFlow;
import br.simplipark.chatbot.nodes.payment.PaymentFlow;
import br.simplipark.user.UserService;
import br.simplipark.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static br.simplipark.chatbot.nodes.MainConversationStage.*;

@Slf4j
@Service
public class MainMenuNode {
    private final QueueMessageDispatcher queueMessageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final LbCoinsPurchaseFlow lbCoinsPurchaseFlow;
    private final ChargeVehicleFlow chargingFlow;
    private final PaymentFlow paymentFlow;
    private final ReportingFlow reportingFlow;

    private final UserService userService;

    private static final String MENU_OPTIONS = """
                1 - Iniciar carga
                2 - Comprar LB Coins
                3 - Ver histórico de cargas desse mês
                4 - Consultar saldo de LB Coins
                """;

    public MainMenuNode(QueueMessageDispatcher queueMessageDispatcher, ConversationPathManager conversationPathManager, LbCoinsPurchaseFlow lbCoinsPurchaseFlow, ChargeVehicleFlow chargingFlow, PaymentFlow paymentFlow, ReportingFlow reportingFlow, UserService userService) {
        this.queueMessageDispatcher = queueMessageDispatcher;
        this.conversationPathManager = conversationPathManager;
        this.lbCoinsPurchaseFlow = lbCoinsPurchaseFlow;
        this.chargingFlow = chargingFlow;
        this.paymentFlow = paymentFlow;
        this.reportingFlow = reportingFlow;
        this.userService = userService;
    }

    public void tryHandleMessage(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        try {
            handleMessage(chatbotUser, chatbotMessage);

        } catch (Exception e) {
            queueMessageDispatcher.clearQueuedMessages(chatbotUser);

            queueMessageDispatcher.queueMessage(chatbotUser, "Desculpe, ocorreu um erro inesperado. Reiniciando o chatbot...");
            queueMessageDispatcher.sendQueuedMessages(chatbotUser);

            conversationPathManager.navigateToImmediately(chatbotUser, MainConversationStage.GREETING.name());

            log.error("Error handling message", e);
        }
    }

    private void handleMessage(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if ("menu".equalsIgnoreCase(chatbotMessage.body())) {
            queueMessageDispatcher.queueMessage(chatbotUser, "Voltando para o menu principal...\n\n");
            conversationPathManager.navigateToImmediately(chatbotUser, MainConversationStage.MAIN_MENU.name());

            return;
        }

        MainConversationStage currentStage = MainConversationStage.valueOf(conversationPathManager.getNextNode(chatbotUser));

        switch (currentStage) {
            case GREETING -> handleGreeting(chatbotUser);
            case ASK_CPF -> handleAskForCpf(chatbotUser, chatbotMessage);
            case MAIN_MENU -> handleMainMenu(chatbotUser, chatbotMessage);
            default -> routeToOtherFlows(currentStage, chatbotUser, chatbotMessage);
        }

        queueMessageDispatcher.sendQueuedMessages(chatbotUser);
    }

    private void handleGreeting(ChatbotUser chatbotUser) {
        queueMessageDispatcher.queueMessage(chatbotUser, "Seja bem vindo(a)!\n");

        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, MainConversationStage.ASK_CPF.name());
    }

    private void handleAskForCpf(ChatbotUser chatbotUser, ChatbotMessage message) {
        if (chatbotUser.sessionData().getUser() != null) {
            conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, MainConversationStage.MAIN_MENU.name());
            return;
        }

        if (message.body().isEmpty()) {
            queueMessageDispatcher.queueMessage(chatbotUser, "Por favor, digite seu CPF para continuarmos.");
            return;
        }

        if (Util.isCpfInvalid(message.body())) {
            queueMessageDispatcher.queueMessage(chatbotUser, "CPF inválido. Por favor, digite um CPF válido.");
            return;
        }

        var user = userService.getUserByCpf(message.body());
        if (user == null) {
            queueMessageDispatcher.queueMessage(chatbotUser, "Usuário não encontrado. Por favor, digite um CPF cadastrado.");
            return;
        }

        chatbotUser.sessionData().setUser(user);

        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, MainConversationStage.MAIN_MENU.name());
    }

    private void handleMainMenu(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if (chatbotMessage.body().isEmpty()) {
            queueMessageDispatcher.queueMessage(chatbotUser,
                    "O que você gostaria de fazer?\n\n" +
                            MENU_OPTIONS + "\n" +
                            "Caso queria voltar para este menu principal, digite 'menu' a qualquer momento.");

            return;
        }

        String invalidOptionMessage = "Opção inválida. Por favor, escolha uma das opções disponíveis.";

        if (Util.isNotInt(chatbotMessage.body())) {
            queueMessageDispatcher.queueMessage(chatbotUser, invalidOptionMessage);

            return;
        }

        int option = Integer.parseInt(chatbotMessage.body());

        MainConversationStage selectedStage = switch (option) {
            case 1 -> CHARGE_VEHICLE;
            case 2 -> LB_COINS_PURCHASE;
            case 3 -> CHARGE_HISTORY;
            case 4 -> BALANCE_INQUIRY;
            default -> null;
        };

        if (selectedStage == null) {
            queueMessageDispatcher.queueMessage(chatbotUser, invalidOptionMessage);

            return;
        }

        conversationPathManager.navigateToImmediately(chatbotUser, selectedStage.name());
    }

    private void routeToOtherFlows(MainConversationStage currentStage, ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        switch (currentStage) {
            case LB_COINS_PURCHASE -> lbCoinsPurchaseFlow.handleMessage(chatbotUser, chatbotMessage);
            case PAYMENT -> paymentFlow.handleMessage(chatbotUser, chatbotMessage);
            case CHARGE_VEHICLE -> chargingFlow.handleMessage(chatbotUser, chatbotMessage);
            case CHARGE_HISTORY -> reportingFlow.handleReportingRequest(chatbotUser);
            case BALANCE_INQUIRY -> reportingFlow.handleBalanceInquiry(chatbotUser);
        }
    }
}
