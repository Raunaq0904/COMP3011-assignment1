package comp3011.assignment1.admin;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.dto.UptimeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Overrides only the actual JVM-terminating step, so this test can verify
    // real accept/conflict logic without killing the test process itself.
    @TestConfiguration
    static class NoOpShutdownConfig {
        @Bean
        @Primary
        ShutdownService testShutdownService(ApplicationContext ctx) {
            return new ShutdownService(ctx) {
                @Override
                protected void performExit() {
                    // Intentionally does nothing in tests.
                }
            };
        }
    }

    @Test
    void uptimeReturnsSaneValues() {
        ResponseEntity<UptimeResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/admin/uptime", UptimeResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().serverUptimeSeconds() >= 0,
                "Uptime should be zero or positive");
    }

    @Test
    void statsReturnsZeroForFreshApplicationContext() {
        ResponseEntity<GlobalStatsResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/global/stats", GlobalStatsResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().inputTokens());
        assertEquals(0, response.getBody().outputTokens());
    }

    @Test
    void shutdownReturns202ThenReturns409OnSecondCall() {
        ResponseEntity<ShutdownResponse> first = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/admin/shutdown", null, ShutdownResponse.class);
        assertEquals(HttpStatus.ACCEPTED, first.getStatusCode());

        ResponseEntity<String> second = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/admin/shutdown", null, String.class);
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    }
}
