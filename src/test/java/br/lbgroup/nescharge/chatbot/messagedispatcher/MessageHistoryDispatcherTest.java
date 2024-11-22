package br.lbgroup.nescharge.chatbot.messagedispatcher;

import br.lbgroup.nescharge.chatbot.ChatbotMessage;
import br.lbgroup.nescharge.test.TestUtils;
import br.lbgroup.commons.util.files.MessageableFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MessageHistoryDispatcherTest {

    private final MockMessageDispatcher mockMessageDispatcher = new MockMessageDispatcher();

    private MessageHistoryDispatcher messageHistoryDispatcher;

    private String historyDirectoryPath;
    
    private final String userContact = "+5511934554764"; 

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
        String outgoingMessage = "Test outgoing message";
        String incomingMessage = "Test incoming message";

        messageHistoryDispatcher.sendMessage(userContact, outgoingMessage);

        Path userFilePath = Paths.get(historyDirectoryPath, userContact + ".txt");
        assertTrue(Files.exists(userFilePath));

        mockMessageDispatcher.simulateMessageReceived(userContact, incomingMessage);

        String fileContent = TestUtils.readTextFromFilePath(userFilePath);
        assertTrue(fileContent.contains(outgoingMessage));
        assertTrue(fileContent.contains(incomingMessage));

        String[] lines = fileContent.split(System.lineSeparator());

        assertEquals(2, lines.length);

        assertTrue(lines[0].contains("Bot: " + outgoingMessage));
        assertTrue(lines[1].contains(userContact + ": " + incomingMessage));

        assertTimestampIsCorrect(lines);
    }

    @Test
    void testCanSendFilesSucessfully() {
        MessageableFile file = new MessageableFile("file-id", "file.txt");
        
        messageHistoryDispatcher.sendFile(userContact, file);

        Path userFilePath = Paths.get(historyDirectoryPath, userContact + ".txt");
        assertTrue(Files.exists(userFilePath));

        String fileContent = TestUtils.readTextFromFilePath(userFilePath);
        assertTrue(fileContent.contains(file.name()));
        assertTrue(fileContent.contains(file.id()));
    }

    @Test
    void testCanSendTemplateMessagesSucessfully() {
        String templateName = "templateName";
        List<String> templateArgs = List.of("arg1", "arg2");

        messageHistoryDispatcher.sendTemplateMessage(userContact, templateName, templateArgs);

        Path userFilePath = Paths.get(historyDirectoryPath, userContact + ".txt");
        assertTrue(Files.exists(userFilePath));

        String fileContent = TestUtils.readTextFromFilePath(userFilePath);
        assertTrue(fileContent.contains(templateName));
        assertTrue(fileContent.contains(templateArgs.get(0)));
        assertTrue(fileContent.contains(templateArgs.get(1)));
    }

    private static void assertTimestampIsCorrect(String[] lines) {
        String[] message = lines[0].split("] ");
        String timestamp = message[0].substring(1);

        try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (Exception e) {
            fail("Timestamp is not in the correct format");
        }
    }

    private static class MockMessageDispatcher implements MessageDispatcher {
        private Consumer<ChatbotMessage> messageHandler;

        @Override
        public void sendMessage(String contact, String message) {
            // Do nothing, this is a mock
        }

        @Override
        public void sendTemplateMessage(String contact, String templateName, List<String> templateArgs) {
            // Do nothing, this is a mock
        }

        @Override
        public void sendLocationRequest(String contact, String description) {
            // Do nothing, this is a mock
        }

        @Override
        public void sendFile(String contact, MessageableFile file) {
            // Do nothing, this is a mock
        }

        @Override
        public void onMessageReceived(Consumer<ChatbotMessage> messageHandler) {
            this.messageHandler = messageHandler;
        }

        public void simulateMessageReceived(String contact, String message) {
            messageHandler.accept(ChatbotMessage.textMessage(contact, message));
        }
    }
}
