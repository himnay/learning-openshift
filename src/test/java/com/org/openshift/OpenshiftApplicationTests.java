package com.org.openshift;

import com.org.openshift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OpenshiftApplicationTests extends AbstractIntegrationTest {

    @Test
    @DisplayName("Spring application context loads successfully with Testcontainers Postgres")
    void contextLoads() {}
}
