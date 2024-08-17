package br.simplipark.chatbot.nodes.lbcoin;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.ChatbotUser;
import br.simplipark.chatbot.ConversationPathManager;
import br.simplipark.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.simplipark.chatbot.nodes.MainConversationStage;
import br.simplipark.payment.PaymentService;
import br.simplipark.payment.currency.LBCoinsConverter;
import br.simplipark.payment.model.PaymentOutcome;
import br.simplipark.user.UserService;
import br.simplipark.util.FormatingUtils;
import br.simplipark.util.Util;
import org.springframework.stereotype.Service;

import static br.simplipark.chatbot.nodes.lbcoin.LbCoinsPurchaseFlowStage.ASK_AMOUNT;
import static br.simplipark.chatbot.nodes.lbcoin.LbCoinsPurchaseFlowStage.CONFIRM_PURCHASE;

@Service
public class LbCoinsPurchaseFlow {
    private final QueueMessageDispatcher messageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final PaymentService paymentService;
    private final UserService userService;

    public LbCoinsPurchaseFlow(QueueMessageDispatcher messageDispatcher, ConversationPathManager conversationPathManager, PaymentService paymentService, UserService userService) {
        this.messageDispatcher = messageDispatcher;

        this.conversationPathManager = conversationPathManager;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    public void handleMessage(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if (!conversationPathManager.hasNextNode(chatbotUser)) {
            conversationPathManager.addNodeToPathImmediately(chatbotUser, ASK_AMOUNT.name());

            return;
        }

        LbCoinsPurchaseFlowStage currentStage = LbCoinsPurchaseFlowStage.valueOf(conversationPathManager.getNextNode(chatbotUser));

        switch (currentStage) {
            case ASK_AMOUNT -> handleLBCoinsPurchase(chatbotUser, chatbotMessage);
            case CONFIRM_PURCHASE -> handlePurchaseConfirmation(chatbotUser, chatbotMessage);
        }
    }

    private void handleLBCoinsPurchase(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if (chatbotMessage.body().isEmpty()) {
            messageDispatcher.queueMessage(chatbotUser, "Você deseja comprar quantas LBCoins?");

            return;
        }

        if (Util.isNotInt(chatbotMessage.body())) {
            handleInvalidLBCoinsAmount(chatbotUser, "Por favor, digite um valor válido.\n\n");

            return;
        }

        int amount = Integer.parseInt(chatbotMessage.body());

        if (amount <= 0) {
            handleInvalidLBCoinsAmount(chatbotUser, "Por favor, digite um valor maior que zero.\n\n");

            return;
        }

        chatbotUser.sessionData().setLbCoinsToPurchase(amount);

        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, CONFIRM_PURCHASE.name());
    }

    private void handlePurchaseConfirmation(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if ("voltar".equalsIgnoreCase(chatbotMessage.body())) {
            conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, ASK_AMOUNT.name());

            return;
        }

        if (!chatbotMessage.body().isEmpty()) {
            messageDispatcher.queueMessage(chatbotUser, "Não entendemos sua mensagem. Iremos gerar um novo link para você.\n\n");
        }

        int amount = chatbotUser.sessionData().getLbCoinsToPurchase();

        var formattedCost = FormatingUtils.roundToTwoDecimals(LBCoinsConverter.convertLBCoinsToBRL(amount));
        messageDispatcher.queueMessage(chatbotUser, "Você escolheu comprar " + amount + " LBCoins. Isso custará R$ " + formattedCost + ".\n\n");

        var paymentLink = paymentService.createLinkForLBCoinsPurchase(chatbotUser.user(), amount, outcome -> onPaymentOutcome(chatbotUser, outcome));

        messageDispatcher.queueMessage(chatbotUser, "Segue o link para efetuar o pagamento: " + paymentLink + "\n\n" +
                "Caso mude de ideia, digite 'voltar' para alterar a quantidade de LB Coins a ser comprada.");
    }

    private void onPaymentOutcome(ChatbotUser chatbotUser, PaymentOutcome outcome) {
        if (outcome == PaymentOutcome.ACCEPTED) {
            messageDispatcher.queueMessage(chatbotUser, "Compra efetuada com sucesso. Seu saldo agora é de " + userService.getLbCoinsBalance(chatbotUser.user()) + " LBCoins.\n\n");
            messageDispatcher.sendQueuedMessages(chatbotUser);

        } else {
            messageDispatcher.queueMessage(chatbotUser, "O pagamento não foi confirmado. Retornando ao menu principal.\n\n");
            conversationPathManager.navigateToImmediately(chatbotUser, MainConversationStage.MAIN_MENU.name());
        }
    }

    private void handleInvalidLBCoinsAmount(ChatbotUser chatbotUser, String message) {
        messageDispatcher.queueMessage(chatbotUser, message);

        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, ASK_AMOUNT.name());
    }
}

