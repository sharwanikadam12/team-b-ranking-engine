package com.placement.teamb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that the Spring application context loads successfully.
 * Uses test properties to avoid connecting to Team C during the build.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "teamc.base-url=http://localhost:9999",
        "teamc.timeout-seconds=5",
        "ranking.writeback.enabled=false"
})
class TeamBApplicationTest {

    @Test
    void contextLoads() {
        // If this test passes, the Spring context wired up without errors.
    }
}
