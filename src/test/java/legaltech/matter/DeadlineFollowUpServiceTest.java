package legaltech.matter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class DeadlineFollowUpServiceTest {
    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-09-14T09:00:00Z"), ZoneOffset.UTC);
        DeadlineFollowUpService service = new DeadlineFollowUpService(clock, 3);

        MatterIntake signedNearDeadline = new MatterIntake(
                "MAT-7", "CLIENT-4", "SIGNED-42", Instant.parse("2026-09-13T12:00:00Z"),
                LocalDate.parse("2026-09-16"));
        MatterIntake unsignedNearDeadline = new MatterIntake(
                "MAT-8", "CLIENT-5", "DRAFT-43", null, LocalDate.parse("2026-09-16"));

        require(service.decide(signedNearDeadline) == DeadlineFollowUpService.Decision.FOLLOW_UP_DUE,
                "Signed delivery near deadline must be followed up");
        require(service.decide(unsignedNearDeadline) == DeadlineFollowUpService.Decision.TRACK_ONLY,
                "Unsigned delivery must remain tracking-only");
        System.out.println("PASS: signed delivery triggers deadline follow-up; unsigned delivery does not");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
