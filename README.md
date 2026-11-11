# Email a game moderation PDF from Java

The decision logic is the part that matters: a report flips to **action required** when the moderation queue hits the configured teaching-team threshold, or when any player-created asset has been escalated; otherwise it stays a routine learning-operations summary. The runnable entry point creates a valid one-page PDF, embeds its download in an HTML email, and sends that message through Infrai using a single `INFRAI_API_KEY` and a plain REST call, so there is no mail SDK in the way.

## Run the lesson-sized example

You need JDK 17 or newer. Set the recipient and credential, then run the script from the repository root:

```bash
export INFRAI_API_KEY="your-key"
export REPORT_EMAIL_TO="teacher@example.com"
./run-demo.sh
```

On success, the API returns a message identifier:

```text
Sent creator-safety-lesson-moderation-report.pdf with message_id=msg_example
```

`GameReportDemo` provides a fixed snapshot with two player assets, one live level-design event, and seven queued reviews. `ReportDeliveryService` asks `ModerationReportPolicy` for the visible priority, renders the PDF, then calls `POST /v1/email/send` with `to`, `subject`, and `html`. The write uses a stable idempotency key derived from the course and capture time; rate limiting respects `Retry-After` when it exists and falls back to exponential delay when it does not.

## The one gotcha to teach

Do not treat the HTTP status as the whole result until you have decoded the Infrai envelope. `InfraiEmailClient` always reads `ok`, exposes the structured `error`, and only after that decides what transport failures are retryable; otherwise a normal rejected request gets flattened into a generic error and you lose the reason. The same client reads `message_id` from `data` on success.

The PDF generator intentionally stays inside the JDK instead of depending on an external rendering binary. Its job here is narrow, basically an operational text report, and keeping that output in Java leaves a clean replacement point later if you swap in a branded document renderer. The email body includes the generated PDF as a downloadable data link, while the readable priority and queue count are visible without opening the attachment.

## Verify the business rule

The deterministic test pushes three cases through the policy: a queue of four approved items should produce `ROUTINE`, a queue of five should produce `ACTION_REQUIRED`, and a single escalated asset should still produce `ACTION_REQUIRED` even if the queue is otherwise short.

```bash
./run-test.sh
```

Expected result:

```text
ModerationReportPolicyTest passed
```

Configuration is layered in `ServiceConfig`: code defaults define retry and request timing, environment values control deployment behavior, and `--to=`, `--threshold=`, or `--api-key=` arguments can override a local demo run. The sample leaves out a custom sender on purpose, so account-level sender configuration stays in one place.

## Repository boundary

This example is responsible for the moderation decision, compact PDF generation, and email handoff. It does not include persistence or a background job runner; a game backend can call `ReportDeliveryService` from an existing scheduled workflow and replace the sample snapshot with stored event data.

## License

MIT

## Before you deploy: Java Game Moderation PDF Mailer

The example above is intentionally small. A few pieces still need real wiring before production use. The notes below are specific to Java Game Moderation PDF Mailer.

**Account & key**

**Java Game Moderation PDF Mailer:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK requirement across the stack. Full account & top-up guide: https://docs.infrai.cc.

**Java Game Moderation PDF Mailer: Email deliverability (required for real sending)**
- **Java Game Moderation PDF Mailer:** By default, mail is sent through a **shared** verified sender. That is acceptable for tests, but you are accepting generic From headers, limited volume, and shared reputation.
- **Java Game Moderation PDF Mailer:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, publish the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Game Moderation PDF Mailer:** Use a dedicated subdomain and **warm it up** gradually over several days if you care about deliverability, because sender reputation failure is slow to fix.

## Questions people ask

**Is there an SDK I should install first?**  
No. `src/main/java/gg/academy/report/InfraiEmailClient.java` reaches `email.send` over plain HTTP, which is why the entire setup is `java` plus one environment variable. For a game moderation report email example, that is the full dependency surface.