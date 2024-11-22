package br.lbgroup.nescharge.chatbot.messagedispatcher;

import br.lbgroup.nescharge.chatbot.ChatbotUser;
import br.lbgroup.commons.util.files.MessageableFile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is deprecated and should not be used.
 * @deprecated Use {@link MessageDispatcher} instead.
 * Reason: Messages should be sent immediately, not queued. The queuing behavior adds unnecessary complexity to the system.
 */
@Deprecated
@Service
public class QueueMessageDispatcher {
    private final Map<ChatbotUser, StringBuilder> userMessages = new HashMap<>();
    private final Map<ChatbotUser, List<MessageableFile>> userFiles = new HashMap<>();

    private final Map<ChatbotUser, List<String>> locationRequests = new HashMap<>();

    private final MessageDispatcher messageDispatcher;

    public QueueMessageDispatcher(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    public void sendFileMessage(ChatbotUser chatbotUser, MessageableFile file) {
        userFiles.putIfAbsent(chatbotUser, new ArrayList<>());

        var files = userFiles.get(chatbotUser);

        files.add(file);
    }

    public void queueMessage(ChatbotUser chatbotUser, String message) {
        userMessages.putIfAbsent(chatbotUser, new StringBuilder());

        var sb = userMessages.get(chatbotUser);

        sb.append(message);
    }

    public void sendLocationRequestMessage(ChatbotUser chatbotUser, String description) {
        locationRequests.putIfAbsent(chatbotUser, new ArrayList<>());

        var requests = locationRequests.get(chatbotUser);

        requests.add(description);
    }

    public void sendQueuedMessages(ChatbotUser contact) {
        sendTextMessages(contact);

        sendFileMessages(contact);

        sendLocationRequests(contact);
    }

    private void sendTextMessages(ChatbotUser contact) {
        var sb = userMessages.get(contact);

        if (sb == null) {
            return;
        }

        var message = sb.toString();

        userMessages.remove(contact);

        messageDispatcher.sendMessage(contact.chatId(), message);
    }

    private void sendFileMessages(ChatbotUser contact) {
        var files = userFiles.get(contact);

        if (files == null) {
            return;
        }

        for (var file : files) {
            messageDispatcher.sendFile(contact.chatId(), file);
        }

        userFiles.remove(contact);
    }

    private void sendLocationRequests(ChatbotUser contact) {
        var requests = locationRequests.get(contact);

        if (requests == null) {
            return;
        }

        for (var request : requests) {
            messageDispatcher.sendLocationRequest(contact.chatId(), request);
        }

        locationRequests.remove(contact);
    }

    public void clearQueuedMessages(ChatbotUser chatbotUser) {
        userMessages.remove(chatbotUser);
    }
}
