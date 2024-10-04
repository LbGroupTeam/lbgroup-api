package br.simplipark.chatbot.nodes.payment;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.ChatbotUser;
import br.simplipark.chatbot.ConversationPathManager;
import br.simplipark.chatbot.messagedispatcher.QueueMessageDispatcher;
import br.simplipark.chatbot.nodes.MainConversationStage;
import br.simplipark.payment.PaymentService;
import br.simplipark.payment.currency.LBCoinsConverter;
import br.simplipark.payment.exceptions.UnsuficientBalanceException;
import br.simplipark.payment.model.Payment;
import br.simplipark.payment.model.PaymentMethod;
import br.simplipark.payment.model.PaymentOutcome;
import br.simplipark.user.User;
import br.simplipark.user.UserService;
import br.simplipark.util.FormatingUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.simplipark.chatbot.nodes.payment.PaymentFlowStage.*;

@Service
public class PaymentFlow {
    private final QueueMessageDispatcher messageDispatcher;

    private final ConversationPathManager conversationPathManager;

    private final PaymentService paymentService;
    private final UserService userService;

    private final PaymentInfoBuilder paymentInfoBuilder;

    private final Map<String, List<Payment>> userPayments = new HashMap<>();

    public PaymentFlow(QueueMessageDispatcher messageDispatcher, ConversationPathManager conversationPathManager, PaymentService paymentService, UserService userService, PaymentInfoBuilder paymentInfoBuilder) {
        this.messageDispatcher = messageDispatcher;
        this.conversationPathManager = conversationPathManager;

        this.paymentService = paymentService;
        this.userService = userService;

        this.paymentInfoBuilder = paymentInfoBuilder;
    }

    public void handleMessage(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if (!conversationPathManager.hasNextNode(chatbotUser)) {
            conversationPathManager.addNodeToPathImmediately(chatbotUser, SHOW_PAYMENT_METHODS.name());

            return;
        }

        PaymentFlowStage currentStage = PaymentFlowStage.valueOf(conversationPathManager.getNextNode(chatbotUser));

        switch (currentStage) {
            case SHOW_PAYMENT_METHODS -> handlePaymentMethodsSelection(chatbotUser, chatbotMessage);
            case LB_COINS_PAYMENT -> handleLBCoinsPayment(chatbotUser, chatbotMessage);
            case CREDIT_CARD_PAYMENT -> handleCreditCardPayment(chatbotUser, chatbotMessage);
        }
    }

    private void handlePaymentMethodsSelection(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        if (chatbotMessage.body().isEmpty()) {
            showPaymentInfo(chatbotUser);

            return;
        }

        int paymentIndex = Integer.parseInt(chatbotMessage.body()) - 1;

        if (paymentIndex < 0 || paymentIndex >= PaymentMethod.values().length) {
            messageDispatcher.queueMessage(chatbotUser, "Opção inválida. Por favor, escolha uma das opções disponíveis.");
            return;
        }

        PaymentMethod paymentMethod = PaymentMethod.values()[paymentIndex];
        goToPaymentMethodHandler(chatbotUser, paymentMethod);
    }

    private void showPaymentInfo(ChatbotUser chatbotUser) {
        var pendingPayments = paymentService.getPendingPayments(chatbotUser.user());
        if (pendingPayments.isEmpty()) {
            messageDispatcher.queueMessage(chatbotUser, "Você não possui pagamentos pendentes. Você será redirecionado ao menu principal.\n\n");

            conversationPathManager.navigateToImmediately(chatbotUser, MainConversationStage.MAIN_MENU.name());

            return;
        }

        userPayments.putIfAbsent(chatbotUser.chatId(), pendingPayments);

        var paymentInfoMessage = paymentInfoBuilder.buildPaymentInfoMessage(pendingPayments);

        messageDispatcher.queueMessage(chatbotUser, paymentInfoMessage + "Qual será a modalidade de pagamento?\n" +
                buildPaymentMethodList());
    }

    private void goToPaymentMethodHandler(ChatbotUser user, PaymentMethod paymentMethod) {
        PaymentFlowStage conversationNode = switch (paymentMethod) {
            case LB_COINS -> LB_COINS_PAYMENT;
            case CREDIT_CARD -> CREDIT_CARD_PAYMENT;
        };

        conversationPathManager.replaceLastPathNodeImmediately(user, conversationNode.name());
    }

    private void handleLBCoinsPayment(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        var payments = userPayments.get(chatbotUser.chatId());
        double totalCostInLbCoins = LBCoinsConverter.convertBRLToLBCoins(PaymentService.calculateTotalCost(payments));

        if (chatbotMessage.body().isEmpty()) {
            showLBCoinsPaymentInfo(chatbotUser, totalCostInLbCoins);

            return;
        }

        if ("voltar".equalsIgnoreCase(chatbotMessage.body())) {
            conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, SHOW_PAYMENT_METHODS.name());

            return;
        }

        if ("confirmar".equalsIgnoreCase(chatbotMessage.body())) {
            continueWithLBCoinsPayment(chatbotUser, payments, totalCostInLbCoins);
        }
    }

    private void showLBCoinsPaymentInfo(ChatbotUser chatbotUser, double totalCostInLbCoins) {
        if (userService.getLbCoinsBalance(chatbotUser.user()) < totalCostInLbCoins) {
            handleUnsuficientLbCoinsBalance(chatbotUser, totalCostInLbCoins);

            return;
        }

        messageDispatcher.queueMessage(chatbotUser, "Você escolheu pagar com LBCoins.\n\n" +
                buildLbCoinsBalanceMessage(chatbotUser.user(), totalCostInLbCoins) +
                "\n\nConfirma o pagamento? (Digite 'confirmar' para confirmar ou 'voltar' para escolher outra forma de pagamento.)");
    }

    private void continueWithLBCoinsPayment(ChatbotUser chatbotUser, List<Payment> payments, double totalCostInLbCoins) {
        try {
            paymentService.handleLBCoinsPayment(chatbotUser.user(), payments);

            handlePaymentSuccessful(chatbotUser);

        } catch (UnsuficientBalanceException e) {
            handleUnsuficientLbCoinsBalance(chatbotUser, totalCostInLbCoins);
        }
    }

    private void handleCreditCardPayment(ChatbotUser chatbotUser, ChatbotMessage chatbotMessage) {
        var payments = userPayments.get(chatbotUser.chatId());

        if (chatbotMessage.body().isEmpty()) {
            messageDispatcher.queueMessage(chatbotUser, "Você escolheu pagar com cartão de crédito. " +
                    "Segue o link da plataforma de pagamentos:\n\n" +
                    paymentService.createLinkForCreditCardPayment(chatbotUser.user(), payment -> onCreditCardPaymentOutcome(chatbotUser, payment.outcome()), payments) + "\n\n" +
                    "Caso deseje voltar para a escolha de métodos de pagamento, digite 'voltar'.");

            return;
        }

        if ("voltar".equalsIgnoreCase(chatbotMessage.body())) {
            conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, SHOW_PAYMENT_METHODS.name());
            return;
        }

        messageDispatcher.queueMessage(chatbotUser, "Não entendemos sua mensagem. Por favor, digite 'voltar' para escolher outra forma de pagamento.");
    }

    private void onCreditCardPaymentOutcome(ChatbotUser chatbotUser, PaymentOutcome paymentOutcome) {
        if (paymentOutcome == PaymentOutcome.ACCEPTED) {
            handlePaymentSuccessful(chatbotUser);
            messageDispatcher.sendQueuedMessages(chatbotUser);

        } else {
            messageDispatcher.queueMessage(chatbotUser, "Pagamento não confirmado. Por favor, tente novamente. Será gerado um novo link para pagamento. Caso o problema persista, contate um de nossos representantes.");
            conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, CREDIT_CARD_PAYMENT.name());
        }
    }

    private void handleUnsuficientLbCoinsBalance(ChatbotUser chatbotUser, double totalCost) {
        messageDispatcher.queueMessage(chatbotUser, "Saldo insuficiente. Por favor, escolha outra forma de pagamento.\n\n" + buildLbCoinsBalanceMessage(chatbotUser.user(), totalCost) + "\n\n");
        conversationPathManager.replaceLastPathNodeImmediately(chatbotUser, SHOW_PAYMENT_METHODS.name());
    }

    private String buildLbCoinsBalanceMessage(User user, double totalCostInLbCoins) {
        return "Saldo atual: " + FormatingUtils.roundToTwoDecimals(userService.getLbCoinsBalance(user)) + " LBCoins.\n" +
                "Custo da recarga: " + FormatingUtils.roundToTwoDecimals(totalCostInLbCoins) + " LBCoins.";
    }

    private void handlePaymentSuccessful(ChatbotUser chatbotUser) {
        messageDispatcher.queueMessage(chatbotUser, "Pagamento confirmado! Obrigado por utilizar nossa plataforma! Até a próxima.");
        conversationPathManager.clearPath(chatbotUser);

        userPayments.remove(chatbotUser.chatId());
    }

    private String buildPaymentMethodList() {
        StringBuilder sb = new StringBuilder();
        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
            sb.append(paymentMethod.ordinal() + 1).append(" - ").append(paymentMethod.getDescription()).append("\n");
        }

        return sb.toString();
    }
}

