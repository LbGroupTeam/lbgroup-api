package br.simplipark.chatbot;

import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
public class ConversationPathManager {
    private final Map<String, List<String>> paths = new HashMap<>();

    private final Map<String, Integer> userCurrentNodeIndexes = new HashMap<>();

    @Setter
    private Consumer<ChatbotUser> onPathChanged;

    public void replaceLastPathNodeImmediately(ChatbotUser user, String node) {
        replaceLastPathNode(user, node);

        callOnPathChangedCallback(user);
    }

    public void navigateToImmediately(ChatbotUser user, String... path) {
        navigateTo(user, path);

        callOnPathChangedCallback(user);
    }

    public void addNodeToPathImmediately(ChatbotUser user, String node) {
        addNodeToPath(user, node);

        callOnPathChangedCallback(user);
    }

    public void replaceLastPathNode(ChatbotUser user, String node) {
        List<String> path = getUserPath(user);

        if (path.isEmpty()) {
            throw new IllegalStateException("Flow list is empty.");
        }

        path.removeLast();
        path.add(node);
    }

    public void navigateTo(ChatbotUser user, String... path) {
        var userPath = getUserPath(user);
        userPath.clear();

        userPath.addAll(Arrays.asList(path));
    }

    public void addNodeToPath(ChatbotUser user, String node) {
        var path = getUserPath(user);

        path.add(node);
    }

    public String getNextNode(ChatbotUser user) {
        userCurrentNodeIndexes.putIfAbsent(user.chatId(), 0);

        var currentIndex = userCurrentNodeIndexes.get(user.chatId());

        var currentPath = getUserPath(user);

        if (currentIndex >= currentPath.size()) {
            throw new IllegalStateException("No more conversation stages to iterate.");
        }

        var currentNode = currentPath.get(currentIndex);

        userCurrentNodeIndexes.put(user.chatId(), currentIndex + 1);

        return currentNode;
    }

    public boolean hasNextNode(ChatbotUser user) {
        userCurrentNodeIndexes.putIfAbsent(user.chatId(), 0);

        var currentIndex = userCurrentNodeIndexes.get(user.chatId());

        var currentPath = getUserPath(user);

        return currentIndex < currentPath.size();
    }

    public void resetCurrentNodeIndex(ChatbotUser user) {
        userCurrentNodeIndexes.put(user.chatId(), 0);
    }

    public void clearPath(ChatbotUser chatbotUser) {
        paths.remove(chatbotUser.chatId());
        userCurrentNodeIndexes.remove(chatbotUser.chatId());
    }

    private void callOnPathChangedCallback(ChatbotUser user) {
        onPathChanged.accept(user);
    }

    private List<String> getUserPath(ChatbotUser user) {
        paths.putIfAbsent(user.chatId(), new ArrayList<>());
        return paths.get(user.chatId());
    }
}
