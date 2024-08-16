package br.simplipark.chatbot;

import java.util.Map;

public record ConversationStage(String name, String readableName, String message, Map<String, String> options, String action) {}