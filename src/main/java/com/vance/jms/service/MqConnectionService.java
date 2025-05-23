package com.vance.jms.service;

import com.vance.jms.config.MqConfig;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Getter
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger currentReconnectAttempts = new AtomicInteger(0);
    private LocalDateTime pausedUntil = null;

    @Autowired
    public MqConnectionService(MqConfig mqConfig, ConnectionFactory connectionFactory) {
        this.mqConfig = mqConfig;
        this.connectionFactory = connectionFactory;
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
            pausedUntil = null; // Reset pause
            currentReconnectAttempts.set(0); // Reset attempts after pause
        }
        
        if (currentReconnectAttempts.get() >= mqConfig.getMaxReconnectAttempts()) {
            log.warn("Max reconnect attempts ({}) reached. Pausing reconnection for {} minutes.",
                    mqConfig.getMaxReconnectAttempts(), mqConfig.getReconnectPauseMinutes());
            pausedUntil = LocalDateTime.now().plus(mqConfig.getReconnectPauseMinutes(), ChronoUnit.MINUTES);
            // Potentially notify other parts of the application about the pause
            // For example, by publishing an application event
            // applicationEventPublisher.publishEvent(new MqConnectionPausedEvent(this, pausedUntil));
            return;
        }

        try (Connection connection = connectionFactory.createConnection()) {
            // The act of creating a connection itself is often enough to verify.
            // For some providers, connection.start() might be needed or an operation like creating a session.
            connection.start(); // Explicitly start to ensure connectivity
            log.info("Successfully established MQ connection.");
            connected.set(true);
            currentReconnectAttempts.set(0); // Reset attempts on successful connection
            pausedUntil = null; // Clear any pause
            // Notify other parts of the application that connection is up
            // For example, by publishing an application event
            // applicationEventPublisher.publishEvent(new MqConnectionEstablishedEvent(this));
        } catch (JMSException e) {
            connected.set(false);
            currentReconnectAttempts.incrementAndGet();
            log.error("Failed to establish MQ connection. Attempt {}/{}. Error: {} - {}",
                    currentReconnectAttempts.get(), mqConfig.getMaxReconnectAttempts(), e.getClass().getName(), e.getMessage());
            // Further error details can be logged if e.getCause() is not null or by logging e itself
            // log.debug("Connection failure stack trace:", e); // For more detailed debugging
            // Schedule next attempt or handle pause logic if max attempts reached by scheduler
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
        } else {
            // Optionally, add a periodic health check even if connected.
            // log.trace("Scheduled task running: MQ connection is active.");
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
            // Optionally allow overriding the pause, or just log and wait.
            // For now, we respect the pause. If immediate override is needed, logic can be added here.
             return;
        }
        // Reset attempts if manual trigger should bypass current count, unless it's in a pause period.
        // If not paused, give it a fresh set of attempts.
        if (pausedUntil == null) {
             log.info("Resetting reconnect attempts for manual trigger.");
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
