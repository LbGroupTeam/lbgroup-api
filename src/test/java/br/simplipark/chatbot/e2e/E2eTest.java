package br.simplipark.chatbot.e2e;

import br.simplipark.chatbot.e2e.integrations.MySqlInitializer;
import br.simplipark.chatbot.e2e.integrations.OCPPMockServer;
import br.simplipark.chatbot.e2e.integrations.RandomSenderNumberProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MySqlInitializer.class)
@ExtendWith(OCPPMockServer.class)
@ExtendWith(RandomSenderNumberProvider.class)
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public @interface E2eTest {
}