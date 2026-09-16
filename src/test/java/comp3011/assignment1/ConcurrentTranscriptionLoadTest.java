package comp3011.assignment1;

import comp3011.assignment1.service.SpeechToTextClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentTranscriptionLoadTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // This test config provides a fake, instant SpeechToTextClient, so the load
    // test never calls the real OpenAI API - keeping it fast, free, and repeatable.
    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        SpeechToTextClient stubSpeechToTextClient() {
            return (audioBytes, filename) -> {
                // Simulate a realistic network wait, similar to a real OpenAI call,
                // without actually depending on any external service.
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "{\"text\":\"stubbed transcription\",\"usage\":{\"input_tokens\":10,\"output_tokens\":2}}";
            };
        }
    }

    @Test
    void handles200PlusConcurrentRequestsWithoutFailureOrExcessiveDelay() {
        int requestCount = 250;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Boolean>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            futures.add(executor.submit(this::sendTranscribeRequest));
        }

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            } catch (Exception e) {
                fail("A request failed or timed out: " + e.getMessage());
            }
        }

        long elapsedMs = System.currentTimeMillis() - start;
        executor.shutdown();

        assertEquals(requestCount, successCount, "All requests should succeed");
        assertTrue(elapsedMs < 5000,
                requestCount + " requests should complete well under 5 seconds using virtual threads, took " + elapsedMs + "ms");
    }

    private boolean sendTranscribeRequest() {
        ByteArrayResource fileResource = new ByteArrayResource("fake audio bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "test.webm";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/transcribe", requestEntity, String.class);

        return response.getStatusCode().is2xxSuccessful();
    }
}
