package br.simplipark.chatbot.messagedispatcher.twilio;

import lombok.Data;

@Data
public class IncomingWhatsAppMessage {
    private String messageSid;
    private String accountSid;

    private String from;
    private String to;

    private String body;

    private String profileName;
    private String waId;

    private String originalRepliedMessageSender;
    private String originalRepliedMessageSid;
}
