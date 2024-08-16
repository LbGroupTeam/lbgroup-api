package br.simplipark.chatbot;

import br.simplipark.chatbot.twilio.TwilioChatbot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatbotService {
    private final Chatbot chatbot = new TwilioChatbot();

    private final Map<String, ConversationStages> userStages = new HashMap<>();

    public ChatbotService() {
        chatbot.listenForMessages(this::handleMessage);
    }

    private void handleMessage(ChatbotMessage chatbotMessage) {
        String user = chatbotMessage.from();

        userStages.putIfAbsent(user, ConversationStages.GREETING);

        handleConversationStage(user, chatbotMessage);
    }

    private void handleConversationStage(String user, ChatbotMessage chatbotMessage) {
        ConversationStages currentStage = userStages.get(user);

        switch (currentStage) {
            case GREETING -> {
                chatbot.sendMessage(user, "Bom dia!! Segue a lista de estações:\n Em qual você gostaria de abastecer?");
                userStages.put(user, ConversationStages.CHARGING_STATION_SELECTION);
            }
            case CHARGING_STATION_SELECTION -> handleChargingStationSelection(user, chatbotMessage);
            case CHARGER_SELECTION -> handleChargerSelection(user, chatbotMessage);
            case IDENTITY_VERIFICATION -> {
                chatbot.sendMessage(user, "Você informou o CPF " + chatbotMessage.body() + ". A carga será iniciada imediatamente. Caso deseje parar a carga, digite 'PARAR'.");
                userStages.put(user, ConversationStages.CHARGING);
            }
            case CHARGING -> {
                if ("PARAR".equalsIgnoreCase(chatbotMessage.body())) {
                    chatbot.sendMessage(user, "Carga finalizada. Segue as informações da carga:\n" + buildChargeInfoMessage() + "\n Qual será a modalidade de pagamento?\n" +
                            "1 - LB Coins\n" +
                            "2 - Cartão de crédito\n");
                    userStages.put(user, ConversationStages.PAYMENT_METHOD_SELECTION);
                } else {
                    chatbot.sendMessage(user, "Não entendemos sua mensagem. A carga está em andamento. Caso deseje parar a carga, digite 'PARAR'.");
                }
            }
            case PAYMENT_METHOD_SELECTION -> {
                chatbot.sendMessage(user, buildPaymentChargingMessage(chatbotMessage.body()));
                userStages.put(user, ConversationStages.PAYMENT_ACCEPTED);
            }
            case PAYMENT_ACCEPTED -> {
                chatbot.sendMessage(user, "Pagamento confirmado! Obrigado por utilizar nossa plataforma! Até a próxima.");
                userStages.put(user, ConversationStages.GREETING);
            }
        }
    }

    private void handleChargerSelection(String user, ChatbotMessage chatbotMessage) {
        chatbot.sendMessage(user, "Você está conectado ao carregador " + chatbotMessage.body() + ". Qual o seu CPF?");
        userStages.put(user, ConversationStages.IDENTITY_VERIFICATION);
    }

    private void handleChargingStationSelection(String user, ChatbotMessage chatbotMessage) {
        chatbot.sendMessage(user, "Você escolheu a estação " + chatbotMessage.body() + ". Segue a lista de carregadores:\n" +
                "Qual o carregador você está conectado?");
        userStages.put(user, ConversationStages.CHARGER_SELECTION);
    }

    private String buildChargeInfoMessage() {
        return """
                Kwh carregados: 10
                Valor total: R$ 20,00
                Data de início: 01/01/2021 10:00
                Data de término: 01/01/2021 10:30
                """;
    }

    public String buildPaymentChargingMessage(String body) {
        if ("1".equals(body)) {
            return "Você escolheu pagar com LB Coins. A carga será iniciada imediatamente.";
        } else if ("2".equals(body)) {
            return "Você escolheu pagar com cartão de crédito. Segue o link da plataforma de pagamentos: " + "https://www.google.com";
        } else {
            return "Opção inválida. Por favor, escolha uma das opções disponíveis.";
        }
    }
}