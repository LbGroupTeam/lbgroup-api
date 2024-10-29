package br.simplipark.chatbot.e2e.integrations;

import br.simplipark.chatbot.messagedispatcher.infobip.IncomingMessageDTO;
import br.simplipark.chatbot.messagedispatcher.infobip.InfobipMessageDispatcher;
import br.simplipark.test.TestUtils;
import br.simplipark.util.Util;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@Service
public class MessageDispatcherTestService {

    private static final String MESSAGE_LISTENER_URL = "/text";
    private static final String SEND_MESSAGE_URL = "/infobip/receive-message";

    private static final WireMockServer wireMockServer = initializeWireMockServer();

    private final MessageAssertionService messageReceiver;

    @Autowired(required = false)
    private MockMvc mockMvc;

    public MessageDispatcherTestService(MessageAssertionService messageReceiver) {
        this.messageReceiver = messageReceiver;

        initializeIncomingMessageListener();
    }

    public void sendMessage(String message) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(SEND_MESSAGE_URL)
                .contentType("application/json")
                .content(createMessageToSend(message))
        );
    }

    private static WireMockServer initializeWireMockServer() {
        log.info("Initializing WireMock server message listener");

        var server = TestUtils.createWireMockServer();

        server.stubFor(post(urlMatching(MESSAGE_LISTENER_URL))
                .willReturn(aResponse()
                        .withStatus(200)));

        server.start();

        initializeApplicationProperties(server.port());

        log.info("WireMock server message listener started on port {}", server.port());

        return server;
    }

    private static void initializeApplicationProperties(int serverPort) {
        String baseUrl = "http://localhost:" + serverPort;

        log.info("Initializing properties and WireMock server with baseUrl '{}'", baseUrl);
        System.setProperty("infobip.base_whatsapp_url", baseUrl);
    }

    private void initializeIncomingMessageListener() {
        wireMockServer.addMockServiceRequestListener((request, response) -> {
            log.debug("Request received at url {}, listener triggered", request.getUrl());

            if (!request.getUrl().equals(MESSAGE_LISTENER_URL)) {
                log.warn("Unexpected request received at url {}", request.getUrl());
                return;
            }

            String messageReceived = parseMessageReceived(request.getBodyAsString());

            messageReceiver.addMessageReceived(messageReceived);
        });
    }

    private static String parseMessageReceived(String jsonBody) {
        return Util.deserialize(jsonBody, MessagePayloadDTO.class).content().text();
    }

    private String createMessageToSend(String message) {
        var messageDto = IncomingMessageDTO.createIncomingMessageDTO(RandomSenderNumberProvider.senderNumber, "12243729586", message);

        return Util.serialize(messageDto);
    }

    private record MessagePayloadDTO(String from, String to, InfobipMessageDispatcher.TextContent content) {
    }
}