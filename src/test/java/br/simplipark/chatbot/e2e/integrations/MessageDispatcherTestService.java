package br.simplipark.chatbot.e2e.integrations;

import br.simplipark.chatbot.messagedispatcher.infobip.IncomingMessageDTO;
import br.simplipark.chatbot.messagedispatcher.infobip.InfobipMessageDispatcher;
import br.simplipark.test.TestUtils;
import br.simplipark.util.HttpUtil;
import br.simplipark.util.Util;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@Service
public class MessageDispatcherTestService {

    private static int applicationPort;

    private static final String MESSAGE_LISTENER_URL = "/text";

    private static final WireMockServer wireMockServer = initializeWireMockServer();

    private final MessageAssertionService messageReceiver;

    public MessageDispatcherTestService(MessageAssertionService messageReceiver) {
        this.messageReceiver = messageReceiver;

        initializeIncomingMessageListener();
    }

    @EventListener
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        log.info("Web server started on port {}, setting port for MessageDispatcherTestService", event.getWebServer().getPort());

        applicationPort = event.getWebServer().getPort();
    }

    public void sendMessage(String message) throws IOException {
        var response = sendMessageThroughHttpRequest(message);

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to send message: " + response.body());
        }
    }

    private HttpResponse<String> sendMessageThroughHttpRequest(String message) throws IOException {
        String json = createMessageToSend(message);

        String sendMessageUrl = String.format("http://localhost:%s/infobip/receive-message", applicationPort);

        log.info("Making request to {}", sendMessageUrl);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(sendMessageUrl))
                .timeout(Duration.ofSeconds(10))
                .method("POST", HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json");

        return HttpUtil.sendSimpleHttpRequest(requestBuilder.build());
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
        var messageDto = IncomingMessageDTO.createIncomingMessageDTO("5511934554764", "12243729586", message);

        return Util.serialize(messageDto);
    }

    public record MessagePayloadDTO(String from, String to, InfobipMessageDispatcher.TextContent content) {
    }
}