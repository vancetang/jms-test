package com.vance.jms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import com.vance.jms.event.ConnectionPausedEvent;
import com.vance.jms.event.ConnectionResumedEvent;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 根據 MQ 連接狀態事件管理 JMS 監聽器的生命週期。
 */
@Slf4j
@Service
public class JmsLifecycleManagerService {
    @Autowired
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    // MqConnectionService 在 handleConnectionResumed 方法中使用
    @Autowired
    private MqConnectionService mqConnectionService;

    /**
     * 初始化方法，在 bean 創建後執行
     * 確保 JMS 監聽器在應用程式啟動時被正確初始化
     */
    @PostConstruct
    public void init() {
        log.info("JmsLifecycleManagerService 初始化中...");
        log.info("當前 JMS 監聽器狀態: {}", jmsListenerEndpointRegistry.isRunning() ? "運行中" : "未運行");

        // 列出所有註冊的監聽器容器
        log.info("當前已註冊的監聽器容器 ID: {}", jmsListenerEndpointRegistry.getListenerContainerIds());

        // 在初始化時，JMS 監聽器可能還沒有被完全註冊，所以這裡不嘗試啟動它們
        // 而是在 JmsTestApplication 的 main 方法中啟動它們
        log.info("JMS 監聽器將在應用程式啟動後自動啟動");
    }

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
            // 確認 MQ 連接狀態
            if (!mqConnectionService.isConnected()) {
                log.warn("MQ 連接狀態檢查失敗，不啟動 JMS 監聽器");
                return;
            }

            // 列出所有註冊的監聽器容器
            log.info("嘗試啟動 JMS 監聽器，當前註冊的監聽器容器:");
            jmsListenerEndpointRegistry.getListenerContainerIds().forEach(id -> {
                MessageListenerContainer container = jmsListenerEndpointRegistry.getListenerContainer(id);
                log.info("監聽器 ID: {}, 運行狀態: {}", id, container != null ? container.isRunning() : "容器為空");
            });

            // 無論是初始連接還是恢復連接，都啟動 JMS 監聽器
            // 對於已啟動的監聽器，start() 是冪等的，不會有副作用
            jmsListenerEndpointRegistry.start(); // 啟動所有已註冊的 JMS 監聽器容器
            log.info("JMS 監聽器已成功啟動。");

            // 再次檢查監聽器狀態
            log.info("啟動後的 JMS 監聽器狀態:");
            jmsListenerEndpointRegistry.getListenerContainerIds().forEach(id -> {
                MessageListenerContainer container = jmsListenerEndpointRegistry.getListenerContainer(id);
                log.info("監聽器 ID: {}, 運行狀態: {}", id, container != null ? container.isRunning() : "容器為空");
            });

            // 特別檢查 mainMessageListener
            MessageListenerContainer mainContainer = jmsListenerEndpointRegistry
                    .getListenerContainer("mainMessageListener");
            if (mainContainer != null) {
                log.info("mainMessageListener 狀態: {}", mainContainer.isRunning() ? "運行中" : "未運行");
                if (!mainContainer.isRunning()) {
                    log.info("嘗試單獨啟動 mainMessageListener");
                    mainContainer.start();
                    log.info("mainMessageListener 啟動後狀態: {}", mainContainer.isRunning() ? "運行中" : "未運行");
                }
            } else {
                log.warn("找不到 mainMessageListener 容器");
            }
        } catch (Exception e) {
            log.error("啟動 JMS 監聽器失敗。", e);
            e.printStackTrace();
        }
    }
}
