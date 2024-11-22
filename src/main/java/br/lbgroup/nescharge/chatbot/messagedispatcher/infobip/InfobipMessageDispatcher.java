package br.lbgroup.nescharge.chatbot.messagedispatcher.infobip;

import br.lbgroup.commons.util.Location;
import br.lbgroup.nescharge.chatbot.ChatbotMessage;
import br.lbgroup.nescharge.chatbot.messagedispatcher.MessageDispatcher;
import br.lbgroup.commons.util.HttpUtil;
import br.lbgroup.commons.util.NetworkProperties;
import br.lbgroup.commons.util.Util;
import br.lbgroup.commons.util.files.MessageableFile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@RestController("infobipMessageDispatcher")
public class InfobipMessageDispatcher implements MessageDispatcher {

    private final List<Consumer<ChatbotMessage>> callbacks = new ArrayList<>();
    private final String apiKey;
    private final String baseUrl;
    private final String senderNumber;

    public InfobipMessageDispatcher(@Value("${infobip.api_key}") String apiKey, @Value("${infobip.base_whatsapp_url}") String baseUrl, @Value("${sender.number}") String senderNumber) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.senderNumber = senderNumber;

        log.info("InfobipMessageDispatcher created with baseUrl: {}, senderNumber: {}", baseUrl, senderNumber);
    }

    @Override
    public void sendMessage(String contact, String message) {
        log.info("Sending message to {}: {}", contact, message);

        MessagePayloadDTO messagePayload = buildMessagePayloadDTO(contact, new TextContent(message));

        sendHttpRequestWithPayload(Util.serialize(messagePayload), baseUrl + "/text");
    }

    @Override
    public void sendTemplateMessage(String contact, String templateName, List<String> templateArgs) {
        log.info("Sending template message to {}: {} with args: {}", contact, templateName, templateArgs);

        var templateMessageDto = TemplateMessageDTO.of(senderNumber, contact, templateName, templateArgs);

        sendHttpRequestWithPayload(Util.serialize(templateMessageDto), baseUrl + "/template");
    }

    @Override
    public void sendLocationRequest(String contact, String description) {
        log.info("Sending location request to {}: {}", contact, description);

        var locationContent = LocationContent.of(description);

        MessagePayloadDTO messagePayload = buildMessagePayloadDTO(contact, locationContent);

        sendHttpRequestWithPayload(Util.serialize(messagePayload), baseUrl + "/interactive/location-request");
    }

    @Override
    public void sendFile(String contact, MessageableFile file) {
        log.info("Sending file to {}: {}", contact, file);

        FileContent fileContent = new FileContent(NetworkProperties.externalHttpsUrl + "/files/" + file.id(), file.name());

        MessagePayloadDTO messagePayload = buildMessagePayloadDTO(contact, fileContent);

        sendHttpRequestWithPayload(Util.serialize(messagePayload), baseUrl + "/document");
    }

    @Override
    public void onMessageReceived(Consumer<ChatbotMessage> messageHandler) {
        callbacks.add(messageHandler);
    }

    @PostMapping(value = "/infobip/receive-message")
    public void receiveMessagePost(@RequestBody IncomingMessageDTO messageDTO) {
        log.info("Received message: {}", messageDTO);

        for (IncomingMessageDTO.Result result : messageDTO.getResults()) {
            if (isMessageInvalid(result)) continue;

            var chatbotMessage = parseChatbotMessage(result);
            for (Consumer<ChatbotMessage> consumer : callbacks) {
                consumer.accept(chatbotMessage);
            }
        }
    }

    private MessagePayloadDTO buildMessagePayloadDTO(String contact, Content content) {
        MessagePayloadDTO messagePayload = new MessagePayloadDTO();
        messagePayload.setFrom(senderNumber);
        messagePayload.setTo(contact);
        messagePayload.setContent(content);
        return messagePayload;
    }

    private void sendHttpRequestWithPayload(String jsonRequestBody, String endpoint) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "App " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        try {
            log.debug("Sending HTTP request: {} with body {}", request, jsonRequestBody);
            HttpResponse<String> response = HttpUtil.sendSimpleHttpRequest(request);
            log.info("Response status code: {}", response.statusCode());
            log.debug("Response body: {}", response.body());
        } catch (IOException e) {
            log.error("Error sending HTTP request", e);
            throw new RuntimeException(e);
        }
    }

    private static boolean isMessageInvalid(IncomingMessageDTO.Result result) {
        if (result.getMessage() == null) {
            log.warn("Received message with null message");
            return true;
        }

        return false;
    }

    private static ChatbotMessage parseChatbotMessage(IncomingMessageDTO.Result result) {
        var message = result.getMessage();

        if (message.getType().equals("TEXT")) {
            return ChatbotMessage.textMessage(result.getFrom(), message.getText());
        }

        if (message.getType().equals("LOCATION")) {
            log.info("Received location message: {}", message);

            var location = new Location(message.getLatitude(), message.getLongitude());
            return ChatbotMessage.locationMessage(result.getFrom(), location);
        }

        log.warn("Received message with unknown type: {}", message.getType());

        throw new IllegalArgumentException("Unknown message type: " + message.getType());
    }

    public record TemplateMessageDTO(List<MessagePayloadDTO> messages) {
        public static TemplateMessageDTO of(String from, String to, String templateName, List<String> placeholders) {
            MessagePayloadDTO messagePayload = new MessagePayloadDTO();

            messagePayload.setTo(to);
            messagePayload.setFrom(from);

            messagePayload.setContent(TextTemplateContent.of(templateName, placeholders, "pt_BR"));

            return new TemplateMessageDTO(List.of(messagePayload));
        }
    }

    @Data
    public static class MessagePayloadDTO {
        private String from;
        private String to;
        private Content content;
    }

    public interface Content {
    }

    public record TextContent(String text) implements Content {
    }

    public record LocationContent(Map<String, Object> body) implements Content {
        public static LocationContent of(String description) {
            Map<String, Object> body = new HashMap<>();
            body.put("text", description);

            return new LocationContent(body);
        }
    }

    public record FileContent(String mediaUrl, String filename) implements Content {
    }

    public record TextTemplateContent(String templateName, Map<String, Object> templateData, String language) implements Content {
        public static TextTemplateContent of(String templateName, List<String> placeholders, String language) {
            Map<String, Object> body = new HashMap<>();
            body.put("placeholders", placeholders);

            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            return new TextTemplateContent(templateName, templateData, language);
        }
    }
}