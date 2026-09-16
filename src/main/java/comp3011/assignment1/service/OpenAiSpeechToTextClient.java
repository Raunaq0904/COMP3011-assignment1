package comp3011.assignment1.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Service
public class OpenAiSpeechToTextClient implements SpeechToTextClient {

    @Value("${OPENAI_API_KEY:NOT_SET}")
    private String openAiApiKey;

    private final RestClient restClient = RestClient.create();

    @Override
    public String transcribe(byte[] audioBytes, String filename) throws IOException {
        ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", "gpt-4o-mini-transcribe");

        return restClient.post()
                .uri("https://api.openai.com/v1/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}