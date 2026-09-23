package legaltech.matter;

import java.time.Instant;
import java.time.LocalDate;

public record MatterIntake(
        String matterId,
        String clientReference,
        String documentReference,
        Instant signedAt,
        LocalDate responseDeadline) {
    public MatterIntake {
        if (matterId.isBlank() || clientReference.isBlank() || documentReference.isBlank()) {
            throw new IllegalArgumentException("Matter, client, and document references are required");
        }
    }
}
