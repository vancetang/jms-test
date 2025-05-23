package com.vance.jms.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

public class ConnectionPausedEvent extends ApplicationEvent {
    private final LocalDateTime pausedUntil;

    public ConnectionPausedEvent(Object source, LocalDateTime pausedUntil) {
        super(source);
        this.pausedUntil = pausedUntil;
    }

    public LocalDateTime getPausedUntil() {
        return pausedUntil;
    }
}
