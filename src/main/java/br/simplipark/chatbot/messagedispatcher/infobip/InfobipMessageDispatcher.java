package br.simplipark.chatbot.messagedispatcher.infobip;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.messagedispatcher.MessageDispatcher;
import br.simplipark.util.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
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
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RestController("infobipMessageDispatcher")
public class InfobipMessageDispatcher implements MessageDispatcher {

    private final List<Consumer<ChatbotMessage>> callbacks = new ArrayList<>();
    private final String apiKey;
    private final String domain;

    private final String senderNumber;

    public InfobipMessageDispatcher(@Value("${infobip.api_key}") String apiKey, @Value("${infobip.domain}") String domain, @Value("${sender.number}") String senderNumber) {
        this.apiKey = apiKey;
        this.domain = domain;
        this.senderNumber = senderNumber;

        log.info("InfobipMessageDispatcher created with apiKey: {}, domain: {}, senderNumber: {}", apiKey, domain, senderNumber);
    }

    @Override
    public void sendMessage(String contact, String message) {
        log.info("Sending message to {}: {}", contact, message);

        MessagePayloadDTO messagePayload = new MessagePayloadDTO();
        messagePayload.setFrom(senderNumber);
        messagePayload.setTo(contact);
        messagePayload.setContent(new Content(message));

        String jsonRequestBody;
        try {
            jsonRequestBody = new ObjectMapper().writeValueAsString(messagePayload);
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON", e);
            throw new RuntimeException(e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + domain + "/whatsapp/1/message/text"))
                .header("Authorization", "App " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        try {
            log.debug("Sending HTTP request: {} with body {}", request, jsonRequestBody);
            HttpResponse<String> response = Util.sendSimpleHttpRequest(request);
            log.info("Response status code: {}", response.statusCode());
            log.debug("Response body: {}", response.body());
        } catch (IOException e) {
            log.error("Error sending HTTP request", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageReceived(Consumer<ChatbotMessage> messageHandler) {
        callbacks.add(messageHandler);
    }

    @PostMapping(value = "/infobip/receive-message")
    public void receiveMessagePost(@RequestBody IncomingMessageDTO messageDTO) {
        log.info("Received message: {}", messageDTO);

        for (IncomingMessageDTO.Result result : messageDTO.getResults()) {
            if (result.getMessage() == null) {
                log.warn("Received message with null message");
                continue;
            }

            String body = result.getMessage().getText();
            if (body == null) {
                log.warn("Received message with null body");
                continue;
            }

            var chatbotMessage = new ChatbotMessage(result.getFrom(), result.getMessage().getText());
            for (Consumer<ChatbotMessage> consumer : callbacks) {
                consumer.accept(chatbotMessage);
            }
        }
    }

    @Data
    public static class MessagePayloadDTO {
        private String from;
        private String to;
        private Content content;
    }

    @Data
    @AllArgsConstructor
    public static class Content {
        private String text;
    }
}