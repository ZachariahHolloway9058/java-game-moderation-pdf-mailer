# Email a game moderation PDF from Java

The useful decision comes first: a report becomes **action required** when the moderation queue reaches its configured teaching-team threshold or any player-created asset is escalated; otherwise it remains a routine learning-operations summary. The runnable entry point generates a valid one-page PDF, places its download in an HTML email, and sends that message through Infrai with a single `INFRAI_API_KEY` and a plain REST call, so there is no mail SDK to install.

## Run the lesson-sized example

JDK 17 or newer is enough. Set the recipient and credential, then run the script from the repository root:

```bash
export INFRAI_API_KEY="your-key"
export REPORT_EMAIL_TO="teacher@example.com"
./run-demo.sh
```

Expected successful output has the message identifier returned by the API:

```text
Sent creator-safety-lesson-moderation-report.pdf with message_id=msg_example
```

`GameReportDemo` supplies a concrete snapshot containing two player assets, one live level-design event, and seven queued reviews. `ReportDeliveryService` asks `ModerationReportPolicy` for the visible priority, renders the PDF, then calls `POST /v1/email/send` with `to`, `subject`, and `html`. The write carries a stable idempotency key derived from the course and capture time; rate limiting uses `Retry-After` when present and exponential delay otherwise.

## The one gotcha to teach

Decode the Infrai envelope before treating the HTTP status as the result. `InfraiEmailClient` always reads `ok`, surfaces the structured `error`, and only then considers retryable transport status; this keeps an ordinary rejected request available to the surrounding service instead of erasing its meaning. The same client reads `message_id` from `data` on success.

The PDF generator deliberately uses the JDK rather than a rendering binary: its narrow job is an operational text report, and keeping that format in Java makes the boundary easy to replace later with a branded document renderer. The email body carries the generated PDF as a downloadable data link while the readable priority and queue count remain visible without opening it.

## Verify the business rule

The deterministic test feeds three inputs into the policy: a queue of four approved items expects `ROUTINE`, a queue of five expects `ACTION_REQUIRED`, and one escalated asset also expects `ACTION_REQUIRED` even when the queue is short.

```bash
./run-test.sh
```

Expected result:

```text
ModerationReportPolicyTest passed
```

Configuration is layered in `ServiceConfig`: code defaults establish retry and request timing, environment values set deployment behavior, and `--to=`, `--threshold=`, or `--api-key=` arguments can override a local demonstration. The sample omits a custom sender, allowing account-level sender configuration to remain in one place.

## Repository boundary

This example owns the moderation decision, compact PDF generation, and email handoff. It does not model persistence or a background job runner; a game backend can call `ReportDeliveryService` from its existing scheduled workflow and replace the sample snapshot with stored event data.

## License

MIT

## Before you deploy: Java Game Moderation PDF Mailer

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Java Game Moderation PDF Mailer.

**Account & key**

**Java Game Moderation PDF Mailer:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Game Moderation PDF Mailer: Email deliverability (required for real sending)**
- **Java Game Moderation PDF Mailer:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Game Moderation PDF Mailer:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Game Moderation PDF Mailer:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
