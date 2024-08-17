package br.simplipark.chatbot;

import br.simplipark.user.User;

public record ChatbotUser(String chatId, SessionData sessionData) {
    public User user() {
        return sessionData.getUser();
    }
}
