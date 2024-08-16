package br.simplipark.chatbot;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConversationFlowBuilder {
    private static final Yaml yamlParser = new Yaml();

    public static final String INITIAL_STAGE_KEY = "start";
    
    public static Map<String, ConversationStage> buildConversationFlow() {
        try (InputStream inputStream = ConversationFlowBuilder.class.getClassLoader().getResourceAsStream("conversation-flow.yaml")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found");
            }

            Map<String, Object> rawData = yamlParser.load(inputStream);

            return parseConversationStages(rawData);
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, ConversationStage> parseConversationStages(Map<String, Object> rawData) {
        Map<String, ConversationStage> stages = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> stageData = (Map<String, Object>) entry.getValue();

            var readableName = (String) stageData.get("readableName");
            var message = (String) stageData.get("message");
            var options = (Map<String, String>) stageData.get("options");
            var action = (String) stageData.get("action");

            ConversationStage stage = new ConversationStage(key, readableName, message, options, action);

            stages.put(key, stage);
        }

        return stages;
    }
}
