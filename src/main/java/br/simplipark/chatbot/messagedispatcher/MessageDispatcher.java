package br.simplipark.chatbot.messagedispatcher;

import br.simplipark.chatbot.ChatbotMessage;

import java.util.function.Consumer;

public interface MessageDispatcher {
    void sendMessage(String contact, String message);
    void onMessageReceived(Consumer<ChatbotMessage> messageHandler);
}