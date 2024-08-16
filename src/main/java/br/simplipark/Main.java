package br.simplipark;

import br.simplipark.chatbot.Chatbot;
import br.simplipark.chatbot.twilio.TwilioChatbot;

public class Main {
    public static void main(String[] args) {
        Chatbot chatbot = new TwilioChatbot();

        chatbot.sendMessage("+5511934554764", "I love potatos, they are amazing, right?");
    }
}