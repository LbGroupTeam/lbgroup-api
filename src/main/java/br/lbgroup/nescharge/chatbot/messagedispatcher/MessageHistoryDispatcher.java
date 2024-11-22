package br.lbgroup.nescharge.chatbot.messagedispatcher;

import br.lbgroup.nescharge.chatbot.ChatbotMessage;
import br.lbgroup.commons.util.FormatingUtils;
import br.lbgroup.commons.util.files.MessageableFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Primary
@Service
public class MessageHistoryDispatcher implements MessageDispatcher {

    private final MessageDispatcher delegateDispatcher;
    private final String historyDirectory;
    private static final String BOT_CONTACT = "Bot";

    public MessageHistoryDispatcher(@Qualifier("infobipMessageDispatcher") MessageDispatcher delegateDispatcher,
                                    @Value("${chatbot.messages.history_directory}") String historyDirectory) {
        this.delegateDispatcher = delegateDispatcher;
        this.historyDirectory = historyDirectory;
    }

    @Override
    public void sendMessage(String contact, String message) {
        log.info("Logging outgoing message for contact: {}", contact);
        logMessageToHistory(contact, formatOutgoingMessage(message));
        delegateDispatcher.sendMessage(contact, message);
    }

    @Override
    public void sendTemplateMessage(String contact, String templateName, List<String> templateArgs) {
        log.info("Logging outgoing template message for contact: {}", contact);

        String message = templateName + " " + templateArgs;

        logMessageToHistory(contact, formatOutgoingMessage(message));
        delegateDispatcher.sendTemplateMessage(contact, templateName, templateArgs);
    }

    @Override
    public void sendLocationRequest(String contact, String description) {
        log.info("Logging outgoing location request for contact: {}", contact);

        String message = "Location request: " + description;

        logMessageToHistory(contact, formatOutgoingMessage(message));
        delegateDispatcher.sendLocationRequest(contact, description);
    }

    @Override
    public void sendFile(String contact, MessageableFile file) {
        log.info("Logging outgoing file for contact: {}", contact);

        String message = file.name() + " (" + file.id() + ")";

        logMessageToHistory(contact, formatOutgoingMessage(message));
        delegateDispatcher.sendFile(contact, file);
    }

    @Override
    public void onMessageReceived(Consumer<ChatbotMessage> messageHandler) {
        Consumer<ChatbotMessage> loggingMessageHandler = chatbotMessage -> {
            log.info("Logging incoming message for contact: {}", chatbotMessage.from());
            logMessageToHistory(chatbotMessage.from(), formatIncomingMessage(chatbotMessage));
            messageHandler.accept(chatbotMessage);
        };

        delegateDispatcher.onMessageReceived(loggingMessageHandler);
    }

    private void logMessageToHistory(String contact, String message) {
        String sanitizedContact = sanitizeContactNumber(contact);
        Path historyFilePath = Path.of(historyDirectory, sanitizedContact + ".txt");

        try {
            Files.createDirectories(historyFilePath.getParent());
            Files.writeString(historyFilePath, message + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to log message for contact {}: {}", contact, e.getMessage());
            throw new RuntimeException("Failed to log message history", e);
        }
    }

    private String formatIncomingMessage(ChatbotMessage chatbotMessage) {
        String contact = chatbotMessage.from();

        String message = chatbotMessage.body();
        if (chatbotMessage.location() != null) {
            message = "Location request: " + chatbotMessage.location();
        }

        return String.format("[%s] %s: %s", getCurrentTimestamp(), contact, message);
    }

    private String formatOutgoingMessage(String message) {
        return String.format("[%s] %s: %s", getCurrentTimestamp(), BOT_CONTACT, message);
    }

    private static String getCurrentTimestamp() {
        return FormatingUtils.formatDate(LocalDateTime.now(), "dd/MM/yyyy HH:mm:ss");
    }

    private static String sanitizeContactNumber(String contact) {
        return contact.replaceAll("[^+0-9]", "");
    }
}
