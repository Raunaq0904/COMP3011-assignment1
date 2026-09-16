package comp3011.assignment1.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import comp3011.assignment1.admin.TokenStats;
import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Service
public class TranscriptionService {

    private final SpeechToTextClient speechToTextClient;
    private final TokenStats tokenStats;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranscriptionService(SpeechToTextClient speechToTextClient, TokenStats tokenStats) {
        this.speechToTextClient = speechToTextClient;
        this.tokenStats = tokenStats;
    }

    public String transcribe(MultipartFile audioFile) throws IOException {
        String responseBody = speechToTextClient.transcribe(
                audioFile.getBytes(), audioFile.getOriginalFilename());

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