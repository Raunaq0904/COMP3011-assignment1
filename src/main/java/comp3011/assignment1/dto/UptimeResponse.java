package comp3011.assignment1.dto;

public record UptimeResponse(
        String utcServerStart,
        String utcNow,
        double serverUptimeSeconds
) {}