package legaltech.matter;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DeadlineFollowUpService {
    public enum Decision { FOLLOW_UP_DUE, TRACK_ONLY }

    private final Clock clock;
    private final int noticeDays;

    public DeadlineFollowUpService(Clock clock, int noticeDays) {
        this.clock = clock;
        this.noticeDays = noticeDays;
    }

    public Decision decide(MatterIntake matter) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(clock), matter.responseDeadline());
        boolean signedDelivery = matter.signedAt() != null && !matter.documentReference().isBlank();
        return signedDelivery && days >= 0 && days <= noticeDays
                ? Decision.FOLLOW_UP_DUE
                : Decision.TRACK_ONLY;
    }
}
