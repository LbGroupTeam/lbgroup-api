package br.simplipark.chatbot;

import br.simplipark.chatbot.messagedispatcher.MessageDispatcher;
import br.simplipark.chatbot.nodes.MainConversationStage;
import br.simplipark.chatbot.nodes.MainMenuNode;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatbotMessageRouter {
    private final ConversationPathManager conversationPathManager;

    private final MainMenuNode mainMenuNode;

    private final Map<String, ChatbotUser> chatUsers = new HashMap<>();

    public ChatbotMessageRouter(MessageDispatcher messageDispatcher, ConversationPathManager conversationPathManager, MainMenuNode mainMenuNode) {
        this.conversationPathManager = conversationPathManager;

        this.conversationPathManager.setOnPathChanged(this::handleRestartOfConversationPath);

        this.mainMenuNode = mainMenuNode;

        messageDispatcher.onMessageReceived(this::handleRawMessage);
    }

    private void handleRawMessage(ChatbotMessage chatbotMessage) {
        chatbotMessage = chatbotMessage.trim();

        ChatbotUser chatbotUser = extractChatbotUserFromMessage(chatbotMessage);

        conversationPathManager.resetCurrentNodeIndex(chatbotUser);

        if (!conversationPathManager.hasNextNode(chatbotUser)) {
            conversationPathManager.addNodeToPathImmediately(chatbotUser, MainConversationStage.GREETING.name());

            return;
        }

        mainMenuNode.tryHandleMessage(chatbotUser, chatbotMessage);
    }

    private ChatbotUser extractChatbotUserFromMessage(ChatbotMessage chatbotMessage) {
        String chatId = chatbotMessage.from();

        chatUsers.putIfAbsent(chatId, new ChatbotUser(chatId, new SessionData()));

        return chatUsers.get(chatId);
    }

    private void handleRestartOfConversationPath(ChatbotUser chatbotUser) {
        conversationPathManager.resetCurrentNodeIndex(chatbotUser);

        mainMenuNode.tryHandleMessage(chatbotUser, ChatbotMessage.emptyBody(chatbotUser.chatId()));
    }
}
