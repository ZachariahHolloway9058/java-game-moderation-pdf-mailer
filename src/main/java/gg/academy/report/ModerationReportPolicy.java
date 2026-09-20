package gg.academy.report;

/** The business decision: when should the report ask the teaching team to act? */
public final class ModerationReportPolicy {
    private final int queueThreshold;

    public ModerationReportPolicy(int queueThreshold) {
        if (queueThreshold < 1) throw new IllegalArgumentException("queueThreshold must be positive");
        this.queueThreshold = queueThreshold;
    }

    public ReportPriority classify(ModerationSnapshot snapshot) {
        boolean hasEscalation = snapshot.assets().stream()
                .anyMatch(asset -> asset.reviewState() == ReviewState.ESCALATED);
        return hasEscalation || snapshot.queuedReviews() >= queueThreshold
                ? ReportPriority.ACTION_REQUIRED
                : ReportPriority.ROUTINE;
    }
}

enum ReportPriority { ROUTINE, ACTION_REQUIRED }
