package comp3011.assignment1.admin;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TokenStats {

    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);

    public void recordUsage(long inputTokenCount, long outputTokenCount) {
        inputTokens.addAndGet(inputTokenCount);
        outputTokens.addAndGet(outputTokenCount);
    }

    public long getInputTokens() {
        return inputTokens.get();
    }

    public long getOutputTokens() {
        return outputTokens.get();
    }
}