package comp3011.assignment1.admin;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.dto.UptimeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
public class AdminController {

    private final ServerStats serverStats;
    private final TokenStats tokenStats;
    private final ShutdownService shutdownService;

    public AdminController(ServerStats serverStats, TokenStats tokenStats, ShutdownService shutdownService) {
        this.serverStats = serverStats;
        this.tokenStats = tokenStats;
        this.shutdownService = shutdownService;
    }

    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();
        Instant start = serverStats.getStartTime();
        double uptimeSeconds = Duration.between(start, now).toMillis() / 1000.0;

        return new UptimeResponse(start.toString(), now.toString(), uptimeSeconds);
    }

    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return new GlobalStatsResponse(tokenStats.getInputTokens(), tokenStats.getOutputTokens());
    }

    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<?> shutdown() {
        boolean accepted = shutdownService.requestShutdown();

        if (accepted) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new ShutdownResponse("Graceful shutdown requested."));
        } else {
            ErrorResponse error = new ErrorResponse(
                    Instant.now().toString(),
                    409,
                    "Conflict",
                    "Graceful shutdown is already in progress.",
                    "/api/v1/admin/shutdown"
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }
}