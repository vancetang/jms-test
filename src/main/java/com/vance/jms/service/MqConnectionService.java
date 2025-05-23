package com.vance.jms.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher; // Added import
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vance.jms.config.MqConfig;
import com.vance.jms.event.ConnectionPausedEvent; // Added import
import com.vance.jms.event.ConnectionResumedEvent; // Added import

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
    private final ConnectionFactory connectionFactory; // 實際的 IBM MQ ConnectionFactory
    private final ApplicationEventPublisher eventPublisher; // 事件發布者

    @Getter
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger currentReconnectAttempts = new AtomicInteger(0);
    private LocalDateTime pausedUntil = null;
    private boolean wasPaused = false; // 追蹤連接是否從暫停狀態恢復

    public MqConnectionService(MqConfig mqConfig, ConnectionFactory connectionFactory,
            ApplicationEventPublisher eventPublisher) {
        this.mqConfig = mqConfig;
        this.connectionFactory = connectionFactory;
        this.eventPublisher = eventPublisher; // 注入的事件發布者
        // 初始連接嘗試
        log.info("MqConnectionService 已初始化。嘗試初始連接...");
        checkAndEstablishConnection();
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
            connected.set(true);

            // 檢查是否是從嘗試或暫停中恢復
            boolean isRecovery = currentReconnectAttempts.get() > 0 || wasPaused;

            currentReconnectAttempts.set(0); // 成功連接時重置嘗試次數
            // 如果我們在暫停後到達這裡，pausedUntil 已經是 null

            // 無論是初始連接還是恢復連接，都發布 ConnectionResumedEvent 事件
            // 這樣 MessageReceiver 就能在應用程式啟動時正確啟動 JMS 監聽器
            eventPublisher.publishEvent(new ConnectionResumedEvent(this, isRecovery));

            if (wasPaused) {
                wasPaused = false; // 重置 wasPaused 標誌
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
        if (!connected.get()) {
            log.info("排程任務運行中: MQ 連接已中斷。嘗試重新連接...");
            checkAndEstablishConnection();
        }
    }

    /**
     * 手動觸發重新連接嘗試。
     * 可以通過 API 端點或其他管理介面調用。
     */
    public void triggerManualReconnect() {
        log.info("已觸發手動重新連接。");
        if (pausedUntil != null && LocalDateTime.now().isBefore(pausedUntil)) {
            log.warn("暫停期間嘗試手動重新連接。暫停結束於 {}。未採取任何行動。",
                    pausedUntil);
            return;
        }
        if (pausedUntil != null && LocalDateTime.now().isAfter(pausedUntil)) {
            log.info("手動觸發發現暫停期剛剛結束。重置以進行新的嘗試。");
            wasPaused = true; // 將此視為從暫停狀態恢復
        }

        // 如果手動觸發應繞過當前計數，則重置嘗試次數，除非處於尚未結束的暫停期。
        if (pausedUntil == null) {
            log.info("為手動觸發重置重新連接嘗試次數（當前未暫停）。");
            currentReconnectAttempts.set(0);
        }
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
