package br.simplipark.chatbot.messagedispatcher.twilio;

import br.simplipark.chatbot.ChatbotMessage;
import br.simplipark.chatbot.messagedispatcher.MessageDispatcher;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@RestController("twilioMessageDispatcher")
public class TwilioMessageDispatcher implements MessageDispatcher {
    private final List<Consumer<ChatbotMessage>> callbacks = new ArrayList<>();

    private final String senderNumber;

    public TwilioMessageDispatcher(@Value("${twilio.account_sid}") String accountSid, @Value("${twilio.auth_token}") String authToken, @Value("${sender.number}") String senderNumber) {
        this.senderNumber = senderNumber;

        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized with account SID: {}", accountSid);
    }

    @Override
    public void sendMessage(String contact, String message) {
        if (!StringUtils.hasText(contact)) {
            log.error("Contact cannot be null or empty");
            throw new IllegalArgumentException("Contact cannot be null or empty");
        }

        contact = contact.replaceAll("[^0-9]", "");
        var to = new PhoneNumber("whatsapp:+" + contact);
        var from = new PhoneNumber("whatsapp:+" + senderNumber);

        Message twilioMessage = Message.creator(to, from, message).create();
        log.info("Sent message to {}: {}", contact, twilioMessage.getBody());
    }

    @Override
    public void onMessageReceived(Consumer<ChatbotMessage> callback) {
        callbacks.add(callback);
        log.info("Callback registered for message reception");
    }

    @PostMapping(value = "/receive-message", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void receiveMessagePost(IncomingWhatsAppMessage message) {
        log.info("Received message: {}", message);

        var chatbotMessage = new ChatbotMessage(message.getFrom(), message.getBody());

        for (Consumer<ChatbotMessage> consumer : callbacks) {
            consumer.accept(chatbotMessage);
            log.info("Processed message for contact {}: {}", message.getFrom(), message.getBody());
        }
    }
}