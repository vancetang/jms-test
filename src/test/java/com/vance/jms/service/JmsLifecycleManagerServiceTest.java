package com.vance.jms.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.config.JmsListenerEndpointRegistry;

import com.vance.jms.event.ConnectionPausedEvent;
import com.vance.jms.event.ConnectionResumedEvent;

@ExtendWith(MockitoExtension.class)
public class JmsLifecycleManagerServiceTest {

    @Mock
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    @InjectMocks
    private JmsLifecycleManagerService jmsLifecycleManagerService;

    // Test Case 1: Handle ConnectionPausedEvent
    @Test
    void testHandleConnectionPausedEvent() {
        ConnectionPausedEvent event = new ConnectionPausedEvent(this, LocalDateTime.now().plusMinutes(30));

        jmsLifecycleManagerService.handleConnectionPaused(event);

        verify(jmsListenerEndpointRegistry, times(1)).stop();
    }

    // Test Case 2: Handle ConnectionResumedEvent (when registry is not running)
    @Test
    void testHandleConnectionResumedEvent_WhenNotRunning() {
        when(jmsListenerEndpointRegistry.isRunning()).thenReturn(false);
        ConnectionResumedEvent event = new ConnectionResumedEvent(this);

        jmsLifecycleManagerService.handleConnectionResumed(event);

        verify(jmsListenerEndpointRegistry, times(1)).start();
    }

    // Test Case 3: Handle ConnectionResumedEvent (when registry is already running)
    @Test
    void testHandleConnectionResumedEvent_WhenAlreadyRunning() {
        when(jmsListenerEndpointRegistry.isRunning()).thenReturn(true);
        ConnectionResumedEvent event = new ConnectionResumedEvent(this);

        jmsLifecycleManagerService.handleConnectionResumed(event);

        verify(jmsListenerEndpointRegistry, never()).start();
    }
}
