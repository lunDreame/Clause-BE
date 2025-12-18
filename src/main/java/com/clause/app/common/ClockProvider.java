package com.clause.app.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Component
public class ClockProvider {
    private Clock clock = Clock.systemDefaultZone();

    public Instant now() {
        return clock.instant();
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void reset() {
        this.clock = Clock.systemDefaultZone();
    }
}

