package br.lbgroup.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;

@Slf4j
@Component
public class NetworkProperties {
    public static String externalIpAddress;
    public static String externalHttpsUrl;

    public NetworkProperties(@Value("${external_ip_address:}") String externalIpAddressFromEnv) {
        externalIpAddress = fetchCurrentExternalIpAddress(externalIpAddressFromEnv);
        externalHttpsUrl = "https://" + externalIpAddress;
    }

    private String fetchCurrentExternalIpAddress(String externalIpAddressFromEnv) {
        if (externalIpAddressFromEnv != null && !externalIpAddressFromEnv.isBlank()) {
            log.info("Using external IP address from environment variable: {}", externalIpAddressFromEnv);
            return externalIpAddressFromEnv;
        }

        try {
            log.info("Starting request to get external IP address...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://checkip.amazonaws.com"))
                    .GET()
                    .build();

            log.debug("Sending request to: {}", request.uri());

            var response = HttpUtil.sendSimpleHttpRequest(request);

            log.debug("Response received: {}", response.body());

            String externalIP = response.body().trim();

            log.info("External IP Address retrieved successfully: {}", externalIP);

            return externalIP;
        } catch (Exception e) {
            log.error("An error occurred while retrieving the external IP address", e);
        }

        throw new RuntimeException("Error retrieving external IP address. Check if internet connectivity is available.");
    }
}