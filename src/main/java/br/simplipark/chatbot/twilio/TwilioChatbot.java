package br.simplipark.chatbot.twilio;

import br.simplipark.chatbot.Chatbot;
import br.simplipark.chatbot.ChatbotMessage;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@RestController
public class TwilioChatbot implements Chatbot {
    public static final String ACCOUNT_SID = "";
    public static final String AUTH_TOKEN = "";

    public static List<Consumer<ChatbotMessage>> callback = new ArrayList<>();

    public TwilioChatbot() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    @Override
    public void sendMessage(String contact, String message) {
        if (contact == null || contact.isBlank()) {
            throw new IllegalArgumentException("Contact cannot be null or empty");
        }

        contact = contact.replaceAll("[^0-9]", "");

        var to = new PhoneNumber("whatsapp:+" + contact);
        var from = new PhoneNumber("whatsapp:+14155238886");

        Message twilioMessage = Message.creator(to, from, message).create();

        System.out.println(twilioMessage.getBody());
    }

    @Override
    public void listenForMessages(Consumer<ChatbotMessage> callback) {
        TwilioChatbot.callback.add(callback);
    }

    @PostMapping(value = "/receive-message", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void receiveMessagePost(IncomingWhatsAppMessage message) {
        System.out.println("Received message");

        System.out.println("message = " + message);

        var chabotMessage = new ChatbotMessage(message.getFrom(), message.getBody());

        for (Consumer<ChatbotMessage> consumer : callback) {
            consumer.accept(chabotMessage);
        }
    }
}