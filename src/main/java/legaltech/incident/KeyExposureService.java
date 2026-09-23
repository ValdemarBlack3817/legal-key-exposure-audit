package legaltech.incident;

import legaltech.infra.InfraiIncidentClient;
import legaltech.matter.DeadlineFollowUpService;
import legaltech.matter.MatterIntake;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KeyExposureService {
    public record IncidentResult(String temporaryKeyId, String auditSearch) {}

    private final InfraiIncidentClient client;
    private final DeadlineFollowUpService deadlines;

    public KeyExposureService(InfraiIncidentClient client, DeadlineFollowUpService deadlines) {
        this.client = client;
        this.deadlines = deadlines;
    }

    public IncidentResult contain(String temporaryKeyId, String incidentId, MatterIntake matter, int graceHours) {
        if (graceHours < 1) throw new IllegalArgumentException("A positive overlap is required");
        if (temporaryKeyId == null || temporaryKeyId.isBlank()) throw new IllegalArgumentException("A disposable key id is required");

        DeadlineFollowUpService.Decision decision = deadlines.decide(matter);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("incident_id", incidentId);
        context.put("temporary_key_id", temporaryKeyId);
        context.put("matter_id", matter.matterId());
        context.put("client_reference", matter.clientReference());
        context.put("document_reference", matter.documentReference());
        context.put("signed_at", matter.signedAt().toString());
        context.put("response_deadline", matter.responseDeadline().toString());
        context.put("follow_up_decision", decision.name());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("message", "legal_key_exposure_containment");
        entry.put("level", "warn");
        entry.put("timestamp", Instant.now().toString());
        entry.put("context", context);

        client.ingest(List.of(entry), incidentId);
        client.reportCompromise(temporaryKeyId);
        client.rotate(temporaryKeyId, graceHours, incidentId);
        String audit = client.search(incidentId);
        return new IncidentResult(temporaryKeyId, audit);
    }
}
