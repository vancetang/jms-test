package com.vance.jms.event;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * 連接恢復事件
 * 當 MQ 連接成功恢復時觸發此事件
 */
public class ConnectionResumedEvent extends ApplicationEvent {
    @Getter
    private final LocalDateTime resumeTime;

    @Getter
    private final boolean recovery;

    /**
     * 建立一個新的連接恢復事件
     *
     * @param source 事件來源
     */
    public ConnectionResumedEvent(Object source) {
        this(source, false);
    }

    /**
     * 建立一個新的連接恢復事件
     *
     * @param source   事件來源
     * @param recovery 是否是從斷線中恢復（true）或初始連接（false）
     */
    public ConnectionResumedEvent(Object source, boolean recovery) {
        super(source);
        this.resumeTime = LocalDateTime.now();
        this.recovery = recovery;
    }
}
