package br.simplipark.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class Util {
    private Util() {
    }

    public static boolean isCpfInvalid(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }

        String cpfNumbers = cpf.replaceAll("[^0-9]", "");

        return cpfNumbers.length() != 11;
    }

    public static long parseCpfToLong(String cpf) {
        if (isCpfInvalid(cpf)) {
            return 0;
        }

        return Long.parseLong(cpf.replaceAll("[^0-9]", ""));
    }

    public static boolean isNotInt(String str) {
        if (str == null || str.isBlank()) {
            return true;
        }

        try {
            Integer.parseInt(str);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static HttpResponse<String> sendSimpleHttpRequest(HttpRequest request) throws IOException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            log.error("Thread interrupted while sending HTTP request", e);

            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
