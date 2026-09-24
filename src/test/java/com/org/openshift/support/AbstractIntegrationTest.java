package com.org.openshift.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared Testcontainers Postgres container for all @SpringBootTest / @DataJpaTest
 * classes — one container per JVM fork, reused across subclasses to keep the suite fast.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("openshift_db")
            .withUsername("openshift")
            .withPassword("openshift");
}
