package comp3011.assignment1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${OPENAI_API_KEY:NOT_SET}")
    private String openAiApiKey;

    @GetMapping("/api/hello")
    public String hello() {
        return "Hello from your Spring Boot backend!";
    }

    @GetMapping("/api/debug/key-check")
    public String keyCheck() {
        if (openAiApiKey.equals("NOT_SET")) {
            return "No API key found in environment.";
        }
        return "API key loaded successfully. Length: " + openAiApiKey.length() + " characters.";
    }
}