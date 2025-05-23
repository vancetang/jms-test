package com.vance.jms.event;

import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;

public class ConnectionResumedEvent extends ApplicationEvent {
    private final LocalDateTime resumeTime;

    public ConnectionResumedEvent(Object source) {
        super(source);
        this.resumeTime = LocalDateTime.now();
    }

    public LocalDateTime getResumeTime() {
        return resumeTime;
    }
}
