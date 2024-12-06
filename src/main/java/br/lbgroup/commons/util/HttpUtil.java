package br.lbgroup.commons.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class HttpUtil {
    private static final HttpClient client = HttpClient.newHttpClient();

    private HttpUtil() {
    }

    public static HttpResponse<String> sendSimpleHttpRequest(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            log.error("Thread interrupted while sending HTTP request", e);

            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
