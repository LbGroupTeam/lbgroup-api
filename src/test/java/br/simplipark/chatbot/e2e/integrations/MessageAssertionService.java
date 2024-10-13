package br.simplipark.chatbot.e2e.integrations;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Service
public class MessageAssertionService {
    private final List<String> messagesReceived = new ArrayList<>();

    public void addMessageReceived(String message) {
        messagesReceived.add(message);
    }

    public void assertLastReceivedMessageContains(String expectedSubstring) {
        String lastMessage = getLastMessage();

        assertTrue(lastMessage.contains(expectedSubstring));
    }

    public void assertLastReceivedMessageEquals(String expectedMessage) {
        assertEquals(expectedMessage, getLastMessage());
    }

    private String getLastMessage() {
        if (messagesReceived.isEmpty()) {
            throw new RuntimeException("No messages received");
        }

        return messagesReceived.getLast();
    }
}
