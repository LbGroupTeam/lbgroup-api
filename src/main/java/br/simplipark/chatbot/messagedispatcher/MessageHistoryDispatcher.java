package br.simplipark.chatbot.messagedispatcher;

import br.simplipark.chatbot.ChatbotMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

@Slf4j
@Primary
@Service
public class MessageHistoryDispatcher implements MessageDispatcher {

    private final MessageDispatcher messageDispatcher;
    private final String historyDirectory;
    private static final String BOT_CONTACT = "Bot";

    public MessageHistoryDispatcher(@Qualifier("infobipMessageDispatcher") MessageDispatcher messageDispatcher, 
                                    @Value("${chatbot.messages.history_directory}") String historyDirectory) {
        this.messageDispatcher = messageDispatcher;
        this.historyDirectory = historyDirectory;
    }

    @Override
    public void sendMessage(String contact, String message) {
        log.info("Saving outgoing message to history for contact: {}", contact);
        saveOutgoingMessageToHistory(contact, message);
        messageDispatcher.sendMessage(contact, message);
    }

    @Override
    public void onMessageReceived(Consumer<ChatbotMessage> messageHandler) {
        Consumer<ChatbotMessage> savingToHistoryMessageHandler = chatbotMessage -> {
            log.info("Saving incoming message to history for contact: {}", chatbotMessage.from());
            saveIncomingMessageToHistory(chatbotMessage.from(), chatbotMessage.body());
            messageHandler.accept(chatbotMessage);
        };

        messageDispatcher.onMessageReceived(savingToHistoryMessageHandler);
    }

    private void saveIncomingMessageToHistory(String contact, String message) {
        contact = parseContactNumber(contact);
        String text = createIncomingText(contact, message);
        appendToHistoryFile(contact, text);
    }

    private void saveOutgoingMessageToHistory(String contact, String message) {
        contact = parseContactNumber(contact);
        String text = createOutgoingText(message);
        appendToHistoryFile(contact, text);
    }

    private String createIncomingText(String contact, String message) {
        return String.format("[%s] %s: %s", buildCurrentTimestamp(), contact, message);
    }

    private String createOutgoingText(String message) {
        return String.format("[%s] %s: %s", buildCurrentTimestamp(), BOT_CONTACT, message);
    }

    private void appendToHistoryFile(String contact, String text) {
        var filePath = Path.of(historyDirectory, "/" + contact + ".txt");

        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            log.error("Failed to create directories for history file", e);
            throw new RuntimeException(e);
        }

        try (var fileWriter = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            fileWriter.write(text);
            fileWriter.write(System.lineSeparator());
        } catch (IOException e) {
            log.error("Failed to write to history file for contact: {}", contact, e);
        }
    }

    private static String buildCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static String parseContactNumber(String contact) {
        return contact.replaceAll("[^+0-9]", "");
    }
}
