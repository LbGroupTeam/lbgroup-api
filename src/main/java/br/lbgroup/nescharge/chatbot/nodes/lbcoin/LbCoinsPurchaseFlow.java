package br.lbgroup.nescharge.chatbot.nodes.lbcoin;

import br.lbgroup.nescharge.chatbot.ChatbotMessage;
import br.lbgroup.nescharge.chatbot.ChatbotUser;
import br.lbgroup.nescharge.chatbot.ConversationPathManager;
import br.lbgroup.nescharge.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.lbgroup.nescharge.chatbot.nodes.MainConversationStage;
import br.lbgroup.nescharge.payment.PaymentService;
import br.lbgroup.nescharge.payment.model.PaymentOutcome;
import br.lbgroup.commons.user.UserService;
import br.lbgroup.commons.util.FormatingUtils;
import org.springframework.stereotype.Service;

import static br.lbgroup.nescharge.chatbot.nodes.lbcoin.LbCoinsPurchaseFlowStage.ASK_AMOUNT;
import static br.lbgroup.nescharge.chatbot.nodes.lbcoin.LbCoinsPurchaseFlowStage.CONFIRM_PURCHASE;

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
            conversationPathManager.addNodeToPathImmediately(chatbotUser, CONFIRM_PURCHASE.name());

            return;
        }

        LbCoinsPurchaseFlowStage currentStage = LbCoinsPurchaseFlowStage.valueOf(conversationPathManager.getNextNode(chatbotUser));

        if (currentStage == CONFIRM_PURCHASE) {
            handlePurchaseConfirmation(chatbotUser, chatbotMessage);
        }
    }

    private void handlePurchaseConfirmation(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if (!chatbotMessage.body().isEmpty()) {
            messageDispatcher.queueMessage(chatbotUser, "Não entendemos sua mensagem. Iremos gerar um novo link para você.\n\n");
        }

        var paymentLink = paymentService.createLinkForLBCoinsPurchase(chatbotUser.user(), payment -> onPaymentOutcome(chatbotUser, payment.outcome()));

        messageDispatcher.queueMessage(chatbotUser, "Fornecemos pacotes de LBCoins para você comprar.\n\n" +
                                                    "Segue o link para efetuar o pagamento: \n" + paymentLink + "\n\n" +
                                                    "Ao acessá-lo, você poderá escolher a quantidade de pacotes que deseja comprar. " +
                                                    "Ao finalizar o pagamento, seu saldo será atualizado automaticamente.");
    }

    private void onPaymentOutcome(ChatbotUser chatbotUser, PaymentOutcome outcome) {
        if (outcome == PaymentOutcome.ACCEPTED) {
            String saldo = FormatingUtils.roundToTwoDecimals(userService.getLbCoinsBalance(chatbotUser.user()));

            messageDispatcher.queueMessage(chatbotUser, "Compra efetuada com sucesso. Seu saldo agora é de " + saldo + " LBCoins.\n\n");
            messageDispatcher.sendQueuedMessages(chatbotUser);

            conversationPathManager.navigateTo(chatbotUser, MainConversationStage.GREETING.name());

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

