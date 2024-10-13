package br.simplipark.chatbot.messagedispatcher;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.util.files.MessageableFile;

import java.util.function.Consumer;

public interface MessageDispatcher {
    void sendMessage(String contact, String message);
    void sendFile(String contact, MessageableFile file);
    void onMessageReceived(Consumer<ChatbotMessage> messageHandler);
}