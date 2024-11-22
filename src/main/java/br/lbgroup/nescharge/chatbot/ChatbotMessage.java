package br.lbgroup.nescharge.chatbot;

import br.lbgroup.commons.util.Location;

import java.util.Objects;

public record ChatbotMessage(String from, String body, Location location) {
    public ChatbotMessage {
        Objects.requireNonNull(from);
        Objects.requireNonNull(body);
    }

    public static ChatbotMessage emptyBody(String from) {
        return textMessage(from, "");
    }

    public static ChatbotMessage textMessage(String from, String body) {
        return new ChatbotMessage(from, body, null);
    }

    public static ChatbotMessage locationMessage(String from, Location location) {
        return new ChatbotMessage(from, "", location);
    }

    public ChatbotMessage trim() {
        return new ChatbotMessage(from, body.trim(), location);
    }
}
