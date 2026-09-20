package gg.academy.report;

import java.time.Instant;
import java.util.List;

public final class ModerationReportPolicyTest {
    public static void main(String[] args) {
        ModerationReportPolicy policy = new ModerationReportPolicy(5);
        ModerationSnapshot quiet = snapshot(List.of(
                new PlayerAsset("a-1", "Course badge", ReviewState.APPROVED)), 4);
        ModerationSnapshot busy = snapshot(List.of(
                new PlayerAsset("a-2", "Student arena", ReviewState.PENDING)), 5);
        ModerationSnapshot escalated = snapshot(List.of(
                new PlayerAsset("a-3", "Student banner", ReviewState.ESCALATED)), 1);

        check(policy.classify(quiet) == ReportPriority.ROUTINE, "small healthy queue stays routine");
        check(policy.classify(busy) == ReportPriority.ACTION_REQUIRED, "queue threshold requests action");
        check(policy.classify(escalated) == ReportPriority.ACTION_REQUIRED, "one escalation requests action");
        System.out.println("ModerationReportPolicyTest passed");
    }

    private static ModerationSnapshot snapshot(List<PlayerAsset> assets, int queued) {
        return new ModerationSnapshot("course-1", Instant.EPOCH, assets, List.of(), queued);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
