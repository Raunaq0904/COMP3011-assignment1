package comp3011.assignment1.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TranscriptionControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // The stub behaves differently based on filename, so one bean can cover
    // both the success path and a simulated upstream rejection from OpenAI.
    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        SpeechToTextClient stubSpeechToTextClient() {
            return (audioBytes, filename) -> {
                if ("reject-me.txt".equals(filename)) {
                    throw HttpClientErrorException.create(
                            HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);
                }
                return "{\"text\":\"stubbed transcription\",\"usage\":{\"input_tokens\":5,\"output_tokens\":1}}";
            };
        }
    }

    @Test
    void successfulTranscriptionReturns200WithText() {
        ResponseEntity<String> response = uploadFile("real-audio.webm", "fake audio bytes");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("stubbed transcription"),
                "Response body should contain the transcribed text");
    }

    @Test
    void emptyFileReturns400() {
        ResponseEntity<String> response = uploadFile("empty.txt", "");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("No file received"),
                "Response body should explain the file was empty");
    }

    @Test
    void rejectedFileTypeReturns400() {
        ResponseEntity<String> response = uploadFile("reject-me.txt", "some content");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("rejected the request"),
                "Response body should explain the upstream service rejected the file");
    }

    private ResponseEntity<String> uploadFile(String filename, String content) {
        ByteArrayResource fileResource = new ByteArrayResource(content.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/transcribe", requestEntity, String.class);
    }
}
