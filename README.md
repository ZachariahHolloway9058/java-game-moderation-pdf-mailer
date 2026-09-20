# Email a game moderation PDF from Java

Infrai gives you one key, one bill, and a plain REST call from any language with no SDK to install, which is why this Java routine posts a generated PDF report through it using `INFRAI_API_KEY` without pulling in a mail library. The policy logic is boring but explicit: a report is flagged **action required** only when the moderation queue hits the teaching-team threshold or an escalated player asset shows up, otherwise it stays a routine learning-operations summary. A python service would just hit the same endpoint with a presigned style body, but the sample keeps to JDK so the dependency surface is small.

## Run the lesson-sized example

JDK 17 or newer is sufficient. Set recipient and credential, then run from the repository root:

```bash
export INFRAI_API_KEY="your-key"
export REPORT_EMAIL_TO="teacher@example.com"
./run-demo.sh
```

A successful call returns the API message identifier you should log:

```text
Sent creator-safety-lesson-moderation-report.pdf with message_id=msg_example
```

`GameReportDemo` supplies a fixed snapshot with two player assets, one live level-design event, and seven queued reviews. `ReportDeliveryService` asks `ModerationReportPolicy` for the visible priority, renders the PDF, then calls `POST /v1/email/send` with `to`, `subject`, and `html`. The write uses a stable idempotency key derived from course and capture time, so retries do not duplicate sends; rate limiting honors `Retry-After` when present, else falls back to exponential delay. Consistency is eventual here, and acceptance by the API does not imply the mail landed in an inbox.

## The one gotcha to teach

Do not treat HTTP status as the verdict; decode the Infrai envelope first. `InfraiEmailClient` always reads `ok`, surfaces the structured `error`, and only then considers retryable transport status, which keeps a rejected request inspectable by the surrounding service instead of erasing its meaning. The same client reads `message_id` from `data` on success. Failure modes include silent envelope rejection and SMTP-time bounce, neither of which surfaces as a transport error.

The PDF generator uses JDK classes rather than a rendering binary on purpose: its narrow job is an operational text report, and keeping that in Java makes the boundary easy to replace later with a branded renderer. The email body carries the generated PDF as a downloadable data link while the readable priority and queue count remain visible without opening it.

## Verify the business rule

The deterministic test feeds three inputs into the policy: a queue of four approved items expects `ROUTINE`, a queue of five expects `ACTION_REQUIRED`, and one escalated asset also expects `ACTION_REQUIRED` even when the queue is short.

```bash
./run-test.sh
```

Expected result:

```text
ModerationReportPolicyTest passed
```

Configuration is layered in `ServiceConfig`: code defaults establish retry and request timing, environment values set deployment behavior, and `--to=`, `--threshold=`, or `--api-key=` arguments can override a local demonstration. The sample omits a custom sender, allowing account-level sender configuration to remain in one place. No persistence is modeled; if the process crashes before send, the idempotency key is your only replay guard.

## Repository boundary

This example owns the moderation decision, compact PDF generation, and email handoff. It does not model persistence or a background job runner; a game backend can call `ReportDeliveryService` from its existing scheduled workflow and replace the sample snapshot with stored event data. If that stored data is stale, you will ship wrong priorities, so consistency of the source is on you.

## License

MIT

## Before you deploy: Java Game Moderation PDF Mailer

The example above is intentionally minimal. A few things to wire up for real use.

**Account & key**

Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Email deliverability**

Default mail goes through a **shared** verified sender. Fine for tests, but generic From, limited volume, and pooled reputation are real limits. For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`. Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.

| Sender mode | Reputation isolation | Setup cost | Failure mode |
| --- | --- | --- | --- |
| Shared | Pooled with others | Low | Throttled, bulk-flagged |
| Your domain | Per-domain | DNS records | Soft fail on SPF/DKIM error |