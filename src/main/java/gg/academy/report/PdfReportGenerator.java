package gg.academy.report;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Produces a compact, valid one-page PDF using only the JDK. */
public final class PdfReportGenerator {
    public byte[] generate(ModerationSnapshot snapshot, ReportPriority priority) {
        List<String> lines = new ArrayList<>();
        lines.add("Game Learning Operations Report");
        lines.add("Course: " + snapshot.courseId());
        lines.add("Captured: " + snapshot.capturedAt());
        lines.add("Priority: " + priority);
        lines.add("Moderation queue: " + snapshot.queuedReviews());
        lines.add("Player assets: " + snapshot.assets().size());
        for (PlayerAsset asset : snapshot.assets()) {
            lines.add(asset.id() + " | " + asset.title() + " | " + asset.reviewState());
        }
        lines.add("Live events: " + snapshot.liveEvents().size());
        for (LiveEvent event : snapshot.liveEvents()) {
            lines.add(event.id() + " | " + event.title() + " | attendees " + event.attendees());
        }
        return onePagePdf(lines);
    }

    private byte[] onePagePdf(List<String> lines) {
        StringBuilder stream = new StringBuilder("BT\n/F1 11 Tf\n50 760 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) stream.append("0 -18 Td\n");
            stream.append('(').append(pdfEscape(lines.get(i))).append(") Tj\n");
        }
        stream.append("ET\n");

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + stream.toString().getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + stream + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.US_ASCII).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) pdf.append(String.format("%010d 00000 n \n", offset));
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
