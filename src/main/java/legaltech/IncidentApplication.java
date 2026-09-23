package legaltech;

import legaltech.config.InfraiProperties;
import legaltech.incident.KeyExposureService;
import legaltech.infra.InfraiException;
import legaltech.infra.InfraiIncidentClient;
import legaltech.matter.DeadlineFollowUpService;
import legaltech.matter.MatterIntake;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public final class IncidentApplication {
    private IncidentApplication() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: IncidentApplication <disposable-key-id> <incident-id> <grace-hours>");
            System.exit(2);
        }

        InfraiProperties properties = InfraiProperties.fromEnvironment();
        KeyExposureService service = new KeyExposureService(
                new InfraiIncidentClient(properties),
                new DeadlineFollowUpService(Clock.systemUTC(), 3));
        MatterIntake matter = new MatterIntake(
                "MAT-2048", "CLIENT-88", "SIGNED-NDA-771",
                Instant.now(), LocalDate.now(Clock.systemUTC()).plusDays(2));

        try {
            KeyExposureService.IncidentResult result = service.contain(
                    args[0], args[1], matter, Integer.parseInt(args[2]));
            System.out.println("temporaryKeyId=" + result.temporaryKeyId());
            System.out.println("blastRadius=" + result.auditSearch());
        } catch (InfraiException exception) {
            System.err.println("Infrai rejected the request: code=" + exception.code() + ", status=" + exception.status());
            System.exit(exception.status() >= 400 && exception.status() < 500 ? 2 : 1);
        }
    }
}
