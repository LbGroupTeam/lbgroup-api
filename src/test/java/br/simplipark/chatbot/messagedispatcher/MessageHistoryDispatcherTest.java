package br.simplipark.chatbot.messagedispatcher;

import br.simplipark.chatbot.ChatbotMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MessageHistoryDispatcherTest {

    private final MockMessageDispatcher mockMessageDispatcher = new MockMessageDispatcher();

    private MessageHistoryDispatcher messageHistoryDispatcher;

    private String historyDirectoryPath;

    @BeforeEach
    void setUp() {
        try {
            var tempDirectoryPath = Files.createTempDirectory("test");
            historyDirectoryPath = Path.of(tempDirectoryPath.toString(), "chatbot_message_history_test").toString();
        } catch (IOException e) {
            fail("Failed to create history directory");
        }

        messageHistoryDispatcher = new MessageHistoryDispatcher(mockMessageDispatcher, historyDirectoryPath);
        messageHistoryDispatcher.onMessageReceived(handler -> {});
    }

    @Test
    void testSendingAndReceivingMessage() {
        String userContact = "+5511934554764";
        String outgoingMessage = "Test outgoing message";
        String incomingMessage = "Test incoming message";

        messageHistoryDispatcher.sendMessage(userContact, outgoingMessage);

        Path userFilePath = Paths.get(historyDirectoryPath, userContact + ".txt");
        assertTrue(Files.exists(userFilePath));

        mockMessageDispatcher.simulateMessageReceived(userContact, incomingMessage);

        String fileContent = readTextFromFilePath(userFilePath);
        assertTrue(fileContent.contains(outgoingMessage));
        assertTrue(fileContent.contains(incomingMessage));

        String[] lines = fileContent.split(System.lineSeparator());

        assertEquals(2, lines.length);

        assertTrue(lines[0].contains("Bot: " + outgoingMessage));
        assertTrue(lines[1].contains(userContact + ": " + incomingMessage));

        // Parse the timestamp and check if it is in the correct format
        String[] message = lines[0].split("] ");
        String timestamp = message[0].substring(1);

        try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            fail("Timestamp is not in the correct format");
        }
    }

    private static String readTextFromFilePath(Path userFilePath) {
        String fileContent = null;
        try {
            fileContent = Files.readString(userFilePath);
        } catch (IOException e) {
            fail("Failed to read history file");
        }
        return fileContent;
    }

    private static class MockMessageDispatcher implements MessageDispatcher {
        private Consumer<ChatbotMessage> messageHandler;

        @Override
        public void sendMessage(String contact, String message) {
            // Do nothing, this is a mock
        }

        @Override
        public void onMessageReceived(Consumer<ChatbotMessage> messageHandler) {
            this.messageHandler = messageHandler;
        }

        public void simulateMessageReceived(String contact, String message) {
            messageHandler.accept(new ChatbotMessage(contact, message));
        }
    }
}
