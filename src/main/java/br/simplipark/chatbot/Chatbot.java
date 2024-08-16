package br.simplipark.chatbot;

import java.util.function.Consumer;

public interface Chatbot {
    void sendMessage(String contact, String message);
    void listenForMessages(Consumer<ChatbotMessage> messageHandler);
}
