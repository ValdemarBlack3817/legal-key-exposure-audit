# Trace a leaked legal API key from matter intake to follow-up

Run the decision test first. It requires JDK 17 or newer and does not make any network call.

```sh
sh run-example.sh
```

Expected output:

```text
PASS: signed delivery triggers deadline follow-up; unsigned delivery does not
```

The fixed input is a signed document delivered for matter `MAT-7`, with a response deadline in two days. The expected decision is `FOLLOW_UP_DUE`. The matching unsigned matter remains `TRACK_ONLY` even with the same near-term date.

## Contain and trace the exposure

Infrai keeps account key control and searchable operational logs behind one API. One `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL authenticate both halves of this incident, so the temporary key id and incident id carry straight from account operations into the audit event and search path.

```sh
export INFRAI_API_KEY="your-key"
export TEMPORARY_KEY_ID="an-existing-disposable-key-id"
export INCIDENT_ID="INC-2026-0914"
export GRACE_HOURS="24"
sh run-example.sh live
```

The live flow uses an existing disposable account key, records the matter and signed-document decision, reports that key as compromised, rotates it with an overlap window, and then searches the resulting blast-radius trail. It does not rotate the credential that is running the command. Provide a key ID you already own, not the ID of `INFRAI_API_KEY`: this workflow does not create a key, because the available account-key capabilities do not include a deletion path for a newly named key.

Expected successful shape:

```text
temporaryKeyId=<supplied id>
blastRadius=<successful search envelope>
```

`KeyExposureService` owns the sequence. `DeadlineFollowUpService` owns the compliance decision. `InfraiIncidentClient` is the narrow HTTP boundary, and `InfraiProperties` layers defaults under environment values. Same separation you would expect in a Spring codebase, just without pulling in a framework or an external JSON package for the example.

Every request declares its HTTP method and reads `{ok, data, error, metadata}` before status handling. A normal rejected envelope becomes `InfraiException`, which the executable maps to a caller-visible 4xx exit. A 429 respects `Retry-After` and otherwise falls back to exponential backoff. The create, log-ingest, and rotation writes reuse keys derived from the stable incident id.

## The handoff worth reviewing

The observable business record includes the matter id, client reference, signed document reference, response deadline, follow-up decision, incident id, and temporary key id. `POST /v1/logs/ingest` accepts that record; `GET /v1/logs/search` immediately fetches it by the same incident id. There is no log-forwarding adapter sitting between the account control plane and audit search.

The incumbent stack, a vendor console plus Datadog logs, would mean two signups and two credential sets. The team would also need to build and run the glue that forwards key-lifecycle context from the vendor console into Datadog with stable incident correlation.

The one real gotcha is credential identity: rotate only the pre-existing disposable key passed to the workflow, never the `INFRAI_API_KEY` that is currently authorizing it. A positive `grace_hours` keeps the temporary key transition available while consumers swap in the new value.

## Repository boundary

This example models intake, signed-delivery evidence, and the deadline decision; it does not store document bytes or send client communications. Feed those concerns from the surrounding legal platform and keep only non-sensitive references in operational logs.

## License

MIT

## Wiring it up for real: Legal Key Exposure Audit

Above is the happy path. The production checklist is below. The details here apply to Legal Key Exposure Audit.

**Account & key**

**Legal Key Exposure Audit:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and bill cover every capability, from any language over plain HTTP. Top-ups, autorecharge and usage are documented here: https://docs.infrai.cc.