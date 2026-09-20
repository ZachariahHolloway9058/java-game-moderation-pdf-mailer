package gg.academy.report;

import java.util.Base64;

public final class ReportDeliveryService {
    private final ModerationReportPolicy policy;
    private final PdfReportGenerator pdfGenerator;
    private final InfraiEmailClient emailClient;

    public ReportDeliveryService(
            ModerationReportPolicy policy,
            PdfReportGenerator pdfGenerator,
            InfraiEmailClient emailClient) {
        this.policy = policy;
        this.pdfGenerator = pdfGenerator;
        this.emailClient = emailClient;
    }

    public DeliveryReceipt deliver(String recipient, ModerationSnapshot snapshot) throws Exception {
        ReportPriority priority = policy.classify(snapshot);
        byte[] pdf = pdfGenerator.generate(snapshot, priority);
        String reportName = snapshot.courseId() + "-moderation-report.pdf";
        String pdfData = Base64.getEncoder().encodeToString(pdf);
        String html = "<h1>Game learning operations report</h1>"
                + "<p>Priority: <strong>" + priority + "</strong></p>"
                + "<p>There are " + snapshot.queuedReviews() + " player submissions awaiting review.</p>"
                + "<p><a download=\"" + reportName + "\" href=\"data:application/pdf;base64,"
                + pdfData + "\">Download the generated PDF report</a></p>";
        String subject = priority == ReportPriority.ACTION_REQUIRED
                ? "Action required: game moderation report"
                : "Game moderation report";
        String idempotencyKey = "moderation-report-" + snapshot.courseId() + "-" + snapshot.capturedAt();
        String messageId = emailClient.send(recipient, subject, html, idempotencyKey);
        return new DeliveryReceipt(messageId, reportName, priority);
    }
}

record DeliveryReceipt(String messageId, String reportName, ReportPriority priority) {}
