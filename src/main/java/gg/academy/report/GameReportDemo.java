package gg.academy.report;

import java.time.Instant;
import java.util.List;

/** Explanatory entry point: assemble one moderation snapshot, then deliver it. */
public final class GameReportDemo {
    private GameReportDemo() {}

    public static void main(String[] args) throws Exception {
        ServiceConfig config = ServiceConfig.fromEnvironment(args);
        ModerationSnapshot snapshot = new ModerationSnapshot(
                "creator-safety-lesson",
                Instant.parse("2026-09-17T09:00:00Z"),
                List.of(
                        new PlayerAsset("asset-184", "Player banner", ReviewState.APPROVED),
                        new PlayerAsset("asset-219", "Custom arena", ReviewState.ESCALATED)),
                List.of(new LiveEvent("event-42", "Level Design Lab", 386)),
                7);

        InfraiEmailClient email = InfraiEmailClient.create(config);
        ReportDeliveryService service = new ReportDeliveryService(
                new ModerationReportPolicy(config.escalationThreshold()),
                new PdfReportGenerator(),
                email);

        DeliveryReceipt receipt = service.deliver(config.recipient(), snapshot);
        System.out.println("Sent " + receipt.reportName() + " with message_id=" + receipt.messageId());
    }
}
