package br.simplipark.chatbot.messagedispatcher;

import br.simplipark.chatbot.ChatbotUser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QueueMessageDispatcher {
    private final Map<ChatbotUser, StringBuilder> userMessages = new HashMap<>();

    private final MessageDispatcher messageDispatcher;

    public QueueMessageDispatcher(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    public void queueMessage(ChatbotUser chatbotUser, String message) {
        userMessages.putIfAbsent(chatbotUser, new StringBuilder());

        var sb = userMessages.get(chatbotUser);

        sb.append(message);
    }

    public void sendQueuedMessages(ChatbotUser contact) {
        var sb = userMessages.get(contact);

        if (sb == null) {
            return;
        }

        var message = sb.toString();

        userMessages.remove(contact);

        messageDispatcher.sendMessage(contact.chatId(), message);
    }

    public void clearQueuedMessages(ChatbotUser chatbotUser) {
        userMessages.remove(chatbotUser);
    }
}
