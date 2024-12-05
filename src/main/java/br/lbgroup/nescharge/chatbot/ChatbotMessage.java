package br.lbgroup.nescharge.chatbot;

import java.util.Objects;

public record ChatbotMessage(String from, String body) {
    public ChatbotMessage {
        Objects.requireNonNull(from);
        Objects.requireNonNull(body);
    }

    public static ChatbotMessage emptyBody(String from) {
        return new ChatbotMessage(from, "");
    }

    public ChatbotMessage trim() {
        return new ChatbotMessage(from, body.trim());
    }
}
