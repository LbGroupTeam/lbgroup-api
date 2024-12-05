package br.lbgroup.nescharge.chatbot;

import br.lbgroup.commons.user.User;

public record ChatbotUser(String chatId, SessionData sessionData) {
    public User user() {
        return sessionData.getUser();
    }
}
