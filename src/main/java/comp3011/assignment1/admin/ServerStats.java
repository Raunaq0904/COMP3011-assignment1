package comp3011.assignment1.admin;

import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ServerStats {

    private final Instant startTime = Instant.now();

    public Instant getStartTime() {
        return startTime;
    }
}