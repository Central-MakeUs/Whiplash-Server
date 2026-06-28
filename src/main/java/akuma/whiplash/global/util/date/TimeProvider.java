package akuma.whiplash.global.util.date;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class TimeProvider {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    private final Clock clock;

    public TimeProvider() {
        this(Clock.system(DEFAULT_ZONE));
    }

    public TimeProvider(Clock clock) {
        this.clock = clock.withZone(DEFAULT_ZONE);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(clock.withZone(zoneId));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDate today(ZoneId zoneId) {
        return LocalDate.now(clock.withZone(zoneId));
    }

    public Instant instant() {
        return clock.instant();
    }
}
