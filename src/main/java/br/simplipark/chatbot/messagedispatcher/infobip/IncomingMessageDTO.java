package br.simplipark.chatbot.messagedispatcher.infobip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMessageDTO {
    private List<Result> results;
    private int messageCount;
    private int pendingMessageCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private String from;
        private String to;
        private String integrationType;
        private String receivedAt;
        private String messageId;
        private String pairedMessageId;
        private String callbackData;
        private Message message;
        private Contact contact;
        private Price price;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            private String type;
            private String text;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Contact {
            private String name;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Price {
            private double pricePerMessage;
            private String currency;
        }
    }
}
