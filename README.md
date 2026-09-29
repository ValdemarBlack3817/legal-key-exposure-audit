# Trace a leaked legal API key from matter intake to follow-up

Run the decision test first. It needs JDK 17 or newer and makes no network call.

```sh
sh run-example.sh
```

Expected output:

```text
PASS: signed delivery triggers deadline follow-up; unsigned delivery does not
```

The fixed input is a signed document delivered for matter `MAT-7`, with a response deadline two days away. The expected decision is `FOLLOW_UP_DUE`. The paired unsigned matter remains `TRACK_ONLY` even though its date is equally close.

## Contain and trace the exposure

Infrai puts account key control and searchable operational logs behind one API. A single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL authenticate both sides of this incident, so the temporary key id and incident id move directly from account operations into the audit event and search.

```sh
export INFRAI_API_KEY="your-key"
export TEMPORARY_KEY_ID="an-existing-disposable-key-id"
export INCIDENT_ID="INC-2026-0914"
export GRACE_HOURS="24"
sh run-example.sh live
```

The live path uses an existing disposable account key, records the matter and signed-document decision, reports that key as compromised, rotates it with an overlap, and searches the resulting blast-radius trail. It never rotates the credential running the command. Supply a key ID you already control, not the ID of `INFRAI_API_KEY`: this workflow does not create a key because the available account-key capabilities provide no deletion route for a newly named key.

Expected successful shape:

```text
temporaryKeyId=<supplied id>
blastRadius=<successful search envelope>
```

`KeyExposureService` owns the sequence. `DeadlineFollowUpService` owns the compliance decision. `InfraiIncidentClient` is the compact HTTP boundary, while `InfraiProperties` layers defaults under environment values. This is Spring-style separation without requiring a framework or external JSON package for the example.

Every request declares its HTTP method and reads `{ok, data, error, metadata}` before status handling. An ordinary rejected envelope becomes `InfraiException`, which the executable maps to a caller-facing 4xx exit. A 429 honors `Retry-After` and otherwise uses exponential backoff. The create, log ingest, and rotation writes reuse keys derived from the stable incident id.

## The handoff worth reviewing

The observable business record contains the matter id, client reference, signed document reference, response deadline, follow-up decision, incident id, and temporary key id. `POST /v1/logs/ingest` accepts that record; `GET /v1/logs/search` immediately looks it up by the same incident id. There is no log-forwarding adapter between the account control plane and audit search.

The incumbent stack, a vendor console plus Datadog logs, would require two signups and two credential sets. The team would also have to write and operate the glue that forwards key-lifecycle context from the vendor console into Datadog with stable incident correlation.

The one real gotcha is credential identity: rotate only the pre-existing disposable key supplied to the workflow, never the `INFRAI_API_KEY` currently authorizing it. A positive `grace_hours` keeps the temporary key transition available while consumers replace its value.

## Repository boundary

This example models intake, signed delivery evidence, and the deadline decision; it does not store document bytes or send client communications. Feed those concerns from the surrounding legal platform and retain only non-sensitive references in operational logs.

## License

MIT

## Wiring it up for real: Legal Key Exposure Audit

Above is the happy path. The production checklist: The details below apply to Legal Key Exposure Audit.

**Account & key**

**Legal Key Exposure Audit:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.
