package br.lbgroup.nescharge.chatbot.e2e.integrations;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MySqlInitializer implements BeforeAllCallback {

    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:9.0.1"))
                    .withInitScript("test_database_dump.sql");

    private static final AtomicBoolean hasInitialized = new AtomicBoolean(false);

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

    private void initializeProperties() {
        System.setProperty("spring.datasource.url", mysqlContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mysqlContainer.getUsername());
        System.setProperty("spring.datasource.password", mysqlContainer.getPassword());
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");

        log.info("MySQL properties initialized: url={}, username={}, password={}",
                mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(), mysqlContainer.getPassword());
    }
}
