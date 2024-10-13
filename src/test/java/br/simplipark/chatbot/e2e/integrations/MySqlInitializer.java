package br.simplipark.chatbot.e2e.integrations;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MySqlInitializer implements BeforeAllCallback, AfterAllCallback {

    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:9.0.1"))
                    .withInitScript("test_database_dump.sql");

    private static final AtomicBoolean hasInitialized = new AtomicBoolean(false);

    private final Map<String, String> originalProperties = new HashMap<>();

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        log.info("MySQL initializing...");

        if (hasInitialized.get()) {
            log.info("MySQL already initialized.");

            return;
        }

        mysqlContainer.start();

        initializeProperties();

        hasInitialized.set(true);

        log.info("MySQL initialized.");
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        log.info("MySQL stopping...");

        mysqlContainer.stop();

        rollbackProperties();

        log.info("MySQL stopped.");
    }

    private void initializeProperties() {
        saveOriginalProperty("spring.datasource.url");
        saveOriginalProperty("spring.datasource.username");
        saveOriginalProperty("spring.datasource.password");
        saveOriginalProperty("spring.jpa.hibernate.ddl-auto");

        System.setProperty("spring.datasource.url", mysqlContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mysqlContainer.getUsername());
        System.setProperty("spring.datasource.password", mysqlContainer.getPassword());
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");

        log.info("MySQL properties initialized: url={}, username={}, password={}",
                mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(), mysqlContainer.getPassword());
    }

    private void rollbackProperties() {
        for (Map.Entry<String, String> entry : originalProperties.entrySet()) {
            if (entry.getValue() != null) {
                System.setProperty(entry.getKey(), entry.getValue());
            } else {
                System.clearProperty(entry.getKey());
            }
        }

        log.info("MySQL properties rolled back to original values.");
    }

    private void saveOriginalProperty(String key) {
        originalProperties.put(key, System.getProperty(key));
    }
}
