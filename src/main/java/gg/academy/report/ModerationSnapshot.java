package gg.academy.report;

import java.time.Instant;
import java.util.List;

public record ModerationSnapshot(
        String courseId,
        Instant capturedAt,
        List<PlayerAsset> assets,
        List<LiveEvent> liveEvents,
        int queuedReviews) {
    public ModerationSnapshot {
        assets = List.copyOf(assets);
        liveEvents = List.copyOf(liveEvents);
        if (queuedReviews < 0) throw new IllegalArgumentException("queuedReviews cannot be negative");
    }
}

record PlayerAsset(String id, String title, ReviewState reviewState) {}

record LiveEvent(String id, String title, int attendees) {}

enum ReviewState { APPROVED, PENDING, ESCALATED }
