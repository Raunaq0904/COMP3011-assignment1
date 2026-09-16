package comp3011.assignment1.admin;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.dto.UptimeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
public class AdminController {

    private final ServerStats serverStats;
    private final TokenStats tokenStats;

    public AdminController(ServerStats serverStats, TokenStats tokenStats) {
        this.serverStats = serverStats;
        this.tokenStats = tokenStats;
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
}