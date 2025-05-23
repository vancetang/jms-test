package com.vance.jms.event;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEvent;

/**
 * 連接暫停事件
 * 當 MQ 連接嘗試達到最大次數並進入暫停狀態時觸發此事件
 */
public class ConnectionPausedEvent extends ApplicationEvent {
    private final LocalDateTime pausedUntil;

    /**
     * 建立一個新的連接暫停事件
     *
     * @param source      事件來源
     * @param pausedUntil 暫停結束時間
     */
    public ConnectionPausedEvent(Object source, LocalDateTime pausedUntil) {
        super(source);
        this.pausedUntil = pausedUntil;
    }

    /**
     * 獲取暫停結束時間
     *
     * @return 暫停結束時間
     */
    public LocalDateTime getPausedUntil() {
        return pausedUntil;
    }
}
