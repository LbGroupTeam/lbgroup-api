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

    public static IncomingMessageDTO createIncomingMessageDTO(String from, String to, String text) {
        var result = new Result();
        result.setFrom(from);
        result.setTo(to);
        result.setMessage(new Result.Message("TEXT", text));

        var incomingMessageDTO = new IncomingMessageDTO();
        incomingMessageDTO.setResults(List.of(result));
        incomingMessageDTO.setMessageCount(1);
        incomingMessageDTO.setPendingMessageCount(0);

        return incomingMessageDTO;
    }
}
