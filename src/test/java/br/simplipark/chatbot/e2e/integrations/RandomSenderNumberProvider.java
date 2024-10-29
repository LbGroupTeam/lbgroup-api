package br.simplipark.chatbot.e2e.integrations;

import br.simplipark.test.TestUtils;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class RandomSenderNumberProvider implements BeforeEachCallback {

    public static String senderNumber;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        senderNumber = "55119" + TestUtils.generateRandomNumber(8);
    }
}
