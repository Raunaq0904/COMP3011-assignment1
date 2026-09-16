package comp3011.assignment1.service;

import java.io.IOException;

public interface SpeechToTextClient {
    String transcribe(byte[] audioBytes, String filename) throws IOException;
}