package com.vance.jms.service;

import com.vance.jms.event.ConnectionPausedEvent;
import com.vance.jms.event.ConnectionResumedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jms.listener.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Service;

/**
 * Manages the lifecycle of JMS listeners based on MQ connection status events.
 */
@Slf4j
@Service
public class JmsLifecycleManagerService {

    private final JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    @Autowired
    public JmsLifecycleManagerService(JmsListenerEndpointRegistry jmsListenerEndpointRegistry) {
        this.jmsListenerEndpointRegistry = jmsListenerEndpointRegistry;
    }

    /**
     * Handles the event indicating that MQ connection attempts have been paused.
     * Stops all JMS listeners.
     *
     * @param event The connection paused event.
     */
    @EventListener
    public void handleConnectionPaused(ConnectionPausedEvent event) {
        log.warn("Received ConnectionPausedEvent. MQ reconnection attempts paused until {}. Stopping JMS listeners.", event.getPausedUntil());
        try {
            jmsListenerEndpointRegistry.stop(); // Stops all registered JMS listener containers
            log.info("JMS listeners successfully stopped.");
        } catch (Exception e) {
            log.error("Failed to stop JMS listeners.", e);
        }
    }

    /**
     * Handles the event indicating that MQ connection has been resumed.
     * Starts all JMS listeners.
     *
     * @param event The connection resumed event.
     */
    @EventListener
    public void handleConnectionResumed(ConnectionResumedEvent event) {
        log.info("Received ConnectionResumedEvent at {}. MQ connection has been re-established. Starting JMS listeners.", event.getResumeTime());
        try {
            // Check if listeners are already running, though start() should be idempotent for already started ones.
            if (!jmsListenerEndpointRegistry.isRunning()) {
                jmsListenerEndpointRegistry.start(); // Starts all registered JMS listener containers
                log.info("JMS listeners successfully started.");
            } else {
                log.info("JMS listeners were already running.");
            }
        } catch (Exception e) {
            log.error("Failed to start JMS listeners.", e);
        }
    }
}
