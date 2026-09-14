package comp3011.assignment1.controller;

import comp3011.assignment1.service.TranscriptionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping("/api/transcribe")
    public String receiveAudio(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "No file received.";
        }
        try {
            return transcriptionService.transcribe(file);
        } catch (Exception e) {
            return "Error calling transcription service: " + e.getMessage();
        }
    }
}