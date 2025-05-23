package com.vance.jms.service;

import com.vance.jms.config.MqConfig;
import com.vance.jms.event.ConnectionPausedEvent; // Added import
import com.vance.jms.event.ConnectionResumedEvent; // Added import
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher; // Added import
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service responsible for managing the MQ connection state and handling reconnection logic.
 */
@Slf4j
@Service
public class MqConnectionService {

    private final MqConfig mqConfig;
    private final ConnectionFactory connectionFactory; // Actual IBM MQ ConnectionFactory
    private final ApplicationEventPublisher eventPublisher; // Added field

    @Getter
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger currentReconnectAttempts = new AtomicInteger(0);
    private LocalDateTime pausedUntil = null;
    private boolean wasPaused = false; // To track if connection resumed from a paused state

    @Autowired
    public MqConnectionService(MqConfig mqConfig, ConnectionFactory connectionFactory, ApplicationEventPublisher eventPublisher) {
        this.mqConfig = mqConfig;
        this.connectionFactory = connectionFactory;
        this.eventPublisher = eventPublisher; // Injected
        // Initial connection attempt
        log.info("MqConnectionService initialized. Attempting initial connection...");
        checkAndEstablishConnection();
    }

    /**
     * Checks the current connection status and attempts to establish a connection if not connected.
     * This method can be called manually or by the scheduler.
     */
    public synchronized void checkAndEstablishConnection() {
        if (connected.get()) {
            log.info("MQ connection is already active.");
            return;
        }

        if (pausedUntil != null && LocalDateTime.now().isBefore(pausedUntil)) {
            log.warn("MQ reconnection is paused. Will retry after {}. Current time: {}", pausedUntil, LocalDateTime.now());
            return;
        } else if (pausedUntil != null && LocalDateTime.now().isAfter(pausedUntil)) {
            log.info("MQ reconnection pause has ended. Resuming connection attempts.");
            wasPaused = true; // Mark that we are resuming from a paused state
            pausedUntil = null; // Reset pause
            currentReconnectAttempts.set(0); // Reset attempts after pause
        }
        
        if (currentReconnectAttempts.get() >= mqConfig.getMaxReconnectAttempts()) {
            log.warn("Max reconnect attempts ({}) reached. Pausing reconnection for {} minutes.",
                    mqConfig.getMaxReconnectAttempts(), mqConfig.getReconnectPauseMinutes());
            pausedUntil = LocalDateTime.now().plus(mqConfig.getReconnectPauseMinutes(), ChronoUnit.MINUTES);
            eventPublisher.publishEvent(new ConnectionPausedEvent(this, pausedUntil)); // Publish event
            return;
        }

        try (Connection connection = connectionFactory.createConnection()) {
            connection.start(); // Explicitly start to ensure connectivity
            log.info("Successfully established MQ connection.");
            connected.set(true);
            
            // Only publish ResumedEvent if this is a recovery from attempts or a pause.
            // Not on initial successful connection unless 'wasPaused' or 'currentReconnectAttempts' indicates recovery.
            boolean isRecovery = currentReconnectAttempts.get() > 0 || wasPaused;
            
            currentReconnectAttempts.set(0); // Reset attempts on successful connection
            // pausedUntil is already null if we reached here after a pause
            
            if (isRecovery) {
                 eventPublisher.publishEvent(new ConnectionResumedEvent(this));
                 wasPaused = false; // Reset wasPaused flag
            }
            // No need to reset pausedUntil here as it's handled when pause ends or on success
        } catch (JMSException e) {
            connected.set(false);
            currentReconnectAttempts.incrementAndGet();
            log.error("Failed to establish MQ connection. Attempt {}/{}. Error: {} - {}",
                    currentReconnectAttempts.get(), mqConfig.getMaxReconnectAttempts(), e.getClass().getName(), e.getMessage());
        }
    }

    /**
     * Scheduled task to periodically check and attempt to reconnect if the connection is down.
     * The fixed delay is controlled by MqConfig.reconnectIntervalSeconds.
     */
    @Scheduled(fixedDelayString = "#{@mqConfig.reconnectIntervalSeconds * 1000}")
    public void scheduledReconnectTask() {
        if (!connected.get()) {
            log.info("Scheduled task running: MQ connection is down. Attempting to reconnect...");
            checkAndEstablishConnection();
        }
    }

    /**
     * Manually triggers a reconnection attempt.
     * This can be called via an API endpoint or other management interface.
     */
    public void triggerManualReconnect() {
        log.info("Manual reconnection triggered.");
        if (pausedUntil != null && LocalDateTime.now().isBefore(pausedUntil)) {
            log.warn("Manual reconnection attempt during pause period. Pause ends at {}. No action taken.", pausedUntil);
             return;
        }
        if (pausedUntil != null && LocalDateTime.now().isAfter(pausedUntil)) {
            log.info("Manual trigger finds pause period has just ended. Resetting for fresh attempts.");
            wasPaused = true; // Consider this a resumption from pause state
        }
        
        // Reset attempts if manual trigger should bypass current count, unless it's in a pause period that hasn't ended.
        if (pausedUntil == null) {
             log.info("Resetting reconnect attempts for manual trigger (not currently paused).");
             currentReconnectAttempts.set(0);
        }
        checkAndEstablishConnection();
    }
    
    /**
     * Gets the current connection status.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Gets the current number of reconnection attempts since the last successful connection or pause.
     * @return current reconnection attempts.
     */
    public int getCurrentReconnectAttempts() {
        return currentReconnectAttempts.get();
    }

    /**
     * Gets the time until which reconnection attempts are paused.
     * @return LocalDateTime of when the pause ends, or null if not paused.
     */
    public LocalDateTime getPausedUntil() {
        return pausedUntil;
    }
}
