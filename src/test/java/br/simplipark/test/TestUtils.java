package br.simplipark.test;

import br.simplipark.evcs.chargingdata.ChargingData;
import br.simplipark.evcs.model.Charger;
import br.simplipark.user.User;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class TestUtils {

    private TestUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static User createSampleUser() {
        return new User(1, "56788269050", "VIP");
    }

    public static Charger createSampleCharger() {
        return new Charger("CHARGER 1", "LEFT", null, "SimpliPark", null, null);
    }

    public static ChargingData createSampleChargingData() {
        return createSampleChargingData(1, LocalDateTime.now().minusHours(1), LocalDateTime.now());
    }

    public static ChargingData createSampleChargingData(long id, LocalDateTime start, LocalDateTime end) {
        return new ChargingData(id, Math.random() * 1000 * 1000, start, end);
    }

    public static String readFileFromResources(String fileName) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        Path path = Paths.get(Objects.requireNonNull(classLoader.getResource(fileName)).getPath());
        return Files.readString(path);
    }

    public static String readTextFromFilePath(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    public static WireMockServer createWireMockServer() {
        return new WireMockServer(wireMockConfig()
                .dynamicPort()
                .notifier(new Slf4jNotifier(false))
        );
    }
}