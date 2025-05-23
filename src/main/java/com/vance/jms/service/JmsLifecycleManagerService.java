package com.vance.jms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import com.vance.jms.event.ConnectionPausedEvent;
import com.vance.jms.event.ConnectionResumedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 根據 MQ 連接狀態事件管理 JMS 監聽器的生命週期。
 */
@Slf4j
@Service
public class JmsLifecycleManagerService {
    @Autowired
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    /**
     * 處理表示 MQ 連接嘗試已暫停的事件。
     * 停止所有 JMS 監聽器。
     *
     * @param event 連接暫停事件。
     */
    @EventListener
    public void handleConnectionPaused(ConnectionPausedEvent event) {
        log.warn("收到連接暫停事件。MQ 重新連接嘗試暫停至 {}。正在停止 JMS 監聽器。",
                event.getPausedUntil());
        try {
            jmsListenerEndpointRegistry.stop(); // 停止所有已註冊的 JMS 監聽器容器
            log.info("JMS 監聽器已成功停止。");
        } catch (Exception e) {
            log.error("停止 JMS 監聽器失敗。", e);
        }
    }

    /**
     * 處理表示 MQ 連接已恢復的事件。
     * 啟動所有 JMS 監聽器。
     *
     * @param event 連接恢復事件。
     */
    @EventListener
    public void handleConnectionResumed(ConnectionResumedEvent event) {
        if (event.isRecovery()) {
            log.info(
                    "在 {} 收到連接恢復事件。MQ 連接已重新建立。正在啟動 JMS 監聽器。",
                    event.getResumeTime());
        } else {
            log.info(
                    "在 {} 收到初始連接事件。MQ 連接已建立。正在啟動 JMS 監聽器。",
                    event.getResumeTime());
        }

        try {
            // 檢查監聽器是否已在運行，雖然對於已啟動的監聽器，start() 應該是冪等的。
            if (!jmsListenerEndpointRegistry.isRunning()) {
                jmsListenerEndpointRegistry.start(); // 啟動所有已註冊的 JMS 監聽器容器
                log.info("JMS 監聽器已成功啟動。");
            } else {
                log.info("JMS 監聽器已經在運行中。");
            }
        } catch (Exception e) {
            log.error("啟動 JMS 監聽器失敗。", e);
        }
    }
}
