package comp3011.assignment1.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import comp3011.assignment1.admin.TokenStats;
import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);

    private final SpeechToTextClient speechToTextClient;
    private final TokenStats tokenStats;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranscriptionService(SpeechToTextClient speechToTextClient, TokenStats tokenStats) {
        this.speechToTextClient = speechToTextClient;
        this.tokenStats = tokenStats;
    }

    public String transcribe(MultipartFile audioFile) throws IOException {
        log.info("Received transcription request: filename={}, size={} bytes",
                audioFile.getOriginalFilename(), audioFile.getSize());

        String responseBody = speechToTextClient.transcribe(
                audioFile.getBytes(), audioFile.getOriginalFilename());

        recordTokenUsage(responseBody);

        log.info("Transcription completed successfully for filename={}", audioFile.getOriginalFilename());

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
            log.warn("Could not parse token usage from speech-to-text response: {}", e.getMessage());
        }
    }
}