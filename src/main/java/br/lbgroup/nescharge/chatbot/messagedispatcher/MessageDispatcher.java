package br.lbgroup.nescharge.chatbot.messagedispatcher;

import br.lbgroup.nescharge.chatbot.ChatbotMessage;
import br.lbgroup.commons.util.files.MessageableFile;

import java.util.List;
import java.util.function.Consumer;

public interface MessageDispatcher {
    void sendMessage(String contact, String message);
    void sendTemplateMessage(String contact, String templateName, List<String> templateArgs);
    void sendLocationRequest(String contact, String description);
    void sendFile(String contact, MessageableFile file);
    void onMessageReceived(Consumer<ChatbotMessage> messageHandler);
}