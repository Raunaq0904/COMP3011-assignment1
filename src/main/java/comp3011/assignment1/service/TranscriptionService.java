package comp3011.assignment1.service;

import tools.jackson.databind.ObjectMapper;
import comp3011.assignment1.admin.TokenStats;
import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TranscriptionService {

    @Value("${OPENAI_API_KEY:NOT_SET}")
    private String openAiApiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenStats tokenStats;

    public TranscriptionService(TokenStats tokenStats) {
        this.tokenStats = tokenStats;
    }

    public String transcribe(MultipartFile audioFile) throws IOException {
        ByteArrayResource fileResource = new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                return audioFile.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", "gpt-4o-mini-transcribe");

        String responseBody = restClient.post()
                .uri("https://api.openai.com/v1/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);

        recordTokenUsage(responseBody);

        return responseBody;
    }

    private void recordTokenUsage(String responseBody) {
        try {
            OpenAiTranscriptionResponse parsed =
                    objectMapper.readValue(responseBody, OpenAiTranscriptionResponse.class);
            if (parsed.usage() != null) {
                tokenStats.recordUsage(parsed.usage().inputTokens(), parsed.usage().outputTokens());
            }
        } catch (Exception e) {
            System.err.println("Could not parse token usage from OpenAI response: " + e.getMessage());
        }
    }
}
