package com.vance.jms.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vance.jms.config.MqConfig;
import com.vance.jms.event.ConnectionPausedEvent;
import com.vance.jms.event.ConnectionResumedEvent;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責管理 MQ 連接狀態和處理重新連接邏輯的服務
 */
@Slf4j
@Service
public class MqConnectionService {

    private final MqConfig mqConfig;
    // 實際的 IBM MQ ConnectionFactory
    private final ConnectionFactory connectionFactory;
    // 事件發布者
    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger currentReconnectAttempts = new AtomicInteger(0);
    private LocalDateTime pausedUntil = null;
    // 追蹤連接是否從暫停狀態恢復
    private boolean wasPaused = false;

    public MqConnectionService(MqConfig mqConfig, ConnectionFactory connectionFactory,
            ApplicationEventPublisher eventPublisher) {
        this.mqConfig = mqConfig;
        this.connectionFactory = connectionFactory;
        this.eventPublisher = eventPublisher; // 注入的事件發布者
        // 初始連接嘗試
        log.info("MqConnectionService 已初始化。嘗試初始連接...");

        // 嘗試建立初始連接
        try (Connection connection = connectionFactory.createConnection()) {
            connection.start(); // 顯式啟動以確保連接性
            log.info("成功建立初始 MQ 連接。");
            connected.set(true);

            // 發布初始連接成功事件
            log.info("發布初始連接成功事件");
            eventPublisher.publishEvent(new ConnectionResumedEvent(this, false));

            // 等待一段時間，確保事件被處理
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (JMSException e) {
            connected.set(false);
            currentReconnectAttempts.incrementAndGet();
            log.error("無法建立初始 MQ 連接。錯誤: {} - {}", e.getClass().getName(), e.getMessage());

            // 初始連接失敗，啟動重連機制
            checkAndEstablishConnection();
        }
    }

    /**
     * 檢查當前連接狀態，並在未連接時嘗試建立連接。
     * 此方法可以手動調用或由排程器調用。
     */
    public synchronized void checkAndEstablishConnection() {
        if (connected.get()) {
            log.info("MQ 連接已經處於活動狀態。");
            return;
        }

        log.info("嘗試建立 MQ 連接...");

        if (pausedUntil != null && LocalDateTime.now().isBefore(pausedUntil)) {
            log.warn("MQ 重新連接已暫停。將在 {} 之後重試。當前時間: {}", pausedUntil,
                    LocalDateTime.now());
            return;
        } else if (pausedUntil != null && LocalDateTime.now().isAfter(pausedUntil)) {
            log.info("MQ 重新連接暫停已結束。恢復連接嘗試。");
            wasPaused = true; // 標記我們正在從暫停狀態恢復
            pausedUntil = null; // 重置暫停
            currentReconnectAttempts.set(0); // 暫停後重置嘗試次數
        }

        if (currentReconnectAttempts.get() >= mqConfig.getMaxReconnectAttempts()) {
            log.warn("已達到最大重新連接嘗試次數 ({})。暫停重新連接 {} 分鐘。",
                    mqConfig.getMaxReconnectAttempts(), mqConfig.getReconnectPauseMinutes());
            pausedUntil = LocalDateTime.now().plus(mqConfig.getReconnectPauseMinutes(), ChronoUnit.MINUTES);
            eventPublisher.publishEvent(new ConnectionPausedEvent(this, pausedUntil)); // 發布事件
            return;
        }

        try (Connection connection = connectionFactory.createConnection()) {
            connection.start(); // 顯式啟動以確保連接性
            log.info("成功建立 MQ 連接。");

            // 使用 compareAndSet 確保只有在連接狀態從 false 變為 true 時才發布事件
            if (connected.compareAndSet(false, true)) {
                // 檢查是否是從嘗試或暫停中恢復
                boolean isRecovery = currentReconnectAttempts.get() > 0 || wasPaused;

                currentReconnectAttempts.set(0); // 成功連接時重置嘗試次數
                // 如果我們在暫停後到達這裡，pausedUntil 已經是 null

                // 無論是初始連接還是恢復連接，都發布 ConnectionResumedEvent 事件
                // 這樣 JmsLifecycleManagerService 就能在應用程式啟動時正確啟動 JMS 監聽器
                log.info("發布 ConnectionResumedEvent 事件，isRecovery={}", isRecovery);
                eventPublisher.publishEvent(new ConnectionResumedEvent(this, isRecovery));

                if (wasPaused) {
                    wasPaused = false; // 重置 wasPaused 標誌
                }
            } else {
                log.info("連接狀態已經是 true，不需要發布事件。");
            }
            // 不需要在這裡重置 pausedUntil，因為它在暫停結束或成功時已處理
        } catch (JMSException e) {
            connected.set(false);
            currentReconnectAttempts.incrementAndGet();
            log.error("無法建立 MQ 連接。嘗試 {}/{}。錯誤: {} - {}",
                    currentReconnectAttempts.get(), mqConfig.getMaxReconnectAttempts(), e.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * 定期檢查並在連接中斷時嘗試重新連接的排程任務。
     * 固定延遲由 MqConfig.reconnectIntervalSeconds 控制。
     */
    @Scheduled(fixedDelayString = "#{@mqConfig.reconnectIntervalSeconds * 1000}")
    public void scheduledReconnectTask() {
        // 如果連接已中斷，嘗試重新連接
        if (!connected.get()) {
            log.info("排程任務運行中: MQ 連接已中斷。嘗試重新連接...");
            checkAndEstablishConnection();
        }
    }

    /**
     * 定期檢查 MQ 連接狀態的排程任務。
     * 固定延遲為 10 秒，比重連間隔更頻繁，以便更快地檢測到連接中斷。
     */
    @Scheduled(fixedDelay = 10000) // 每 10 秒檢查一次
    public void scheduledConnectionStatusCheck() {
        log.info("定期連接狀態檢查任務運行中...");

        // 檢查連接狀態
        boolean wasConnected = connected.get();
        checkConnectionStatus();

        // 如果檢查後發現連接已中斷，立即嘗試重連
        if (wasConnected && !connected.get()) {
            log.error("檢測到連接中斷，立即嘗試重連...");
            checkAndEstablishConnection();
        }
    }

    /**
     * 檢查 MQ 連接的實際狀態
     * 此方法會嘗試建立一個測試連接，以確認 MQ 伺服器是否可用
     * 如果連接失敗，會將 connected 設置為 false
     */
    public void checkConnectionStatus() {
        if (!connected.get()) {
            // 如果已知連接中斷，不需要再次檢查
            return;
        }

        log.info("檢查 MQ 連接狀態...");
        try (Connection connection = connectionFactory.createConnection()) {
            // 嘗試啟動連接以確認連接性
            connection.start();
            // 如果成功，不需要做任何事情，連接狀態保持為 true
            log.info("MQ 連接狀態檢查成功，連接正常。");
        } catch (JMSException e) {
            // 連接失敗，將狀態設置為 false
            log.error("MQ 連接狀態檢查失敗，連接已中斷: {} - {}", e.getClass().getName(), e.getMessage());

            // 如果之前是連接狀態，現在檢測到中斷，則發布事件並記錄
            if (connected.compareAndSet(true, false)) {
                log.error("檢測到 MQ 連接中斷！觸發重連機制。");
                // 重置重連嘗試次數，因為這是新的中斷
                currentReconnectAttempts.set(0);
                // 發布連接中斷事件，通知 JmsLifecycleManagerService 停止監聽器
                eventPublisher.publishEvent(new ConnectionPausedEvent(this, null));
            }
        }
    }

    /**
     * 手動觸發重新連接嘗試。
     * 可以通過 API 端點或其他管理介面調用。
     */
    public void triggerManualReconnect() {
        log.info("手動觸發重置重新連接嘗試次數及暫停時間。");
        pausedUntil = null;
        currentReconnectAttempts.set(0);
        checkAndEstablishConnection();
    }

    /**
     * 獲取當前連接狀態。
     *
     * @return 如果已連接則為 true，否則為 false。
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 獲取自上次成功連接或暫停以來的當前重新連接嘗試次數。
     *
     * @return 當前重新連接嘗試次數。
     */
    public int getCurrentReconnectAttempts() {
        return currentReconnectAttempts.get();
    }

    /**
     * 獲取重新連接嘗試暫停的時間。
     *
     * @return 暫停結束的 LocalDateTime，如果未暫停則為 null。
     */
    public LocalDateTime getPausedUntil() {
        return pausedUntil;
    }
}
