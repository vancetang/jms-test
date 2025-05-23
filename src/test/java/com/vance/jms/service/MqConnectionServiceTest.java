package com.vance.jms.service;

import com.vance.jms.config.MqConfig;
import com.vance.jms.event.ConnectionPausedEvent;
import com.vance.jms.event.ConnectionResumedEvent;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqConnectionServiceTest {

    @Mock
    private MqConfig mqConfig;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection mockConnection; // Mock for the actual JMS Connection

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MqConnectionService mqConnectionService;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @BeforeEach
    void setUp() {
        // Default MqConfig values that can be overridden in specific tests
        when(mqConfig.getReconnectIntervalSeconds()).thenReturn(30); // Default, not directly used in these tests
        when(mqConfig.getMaxReconnectAttempts()).thenReturn(3);    // Default for most tests
        when(mqConfig.getReconnectPauseMinutes()).thenReturn(1);    // Default pause
    }

    // Test Case 1: Successful Initial Connection
    @Test
    void testSuccessfulInitialConnection() throws JMSException {
        when(connectionFactory.createConnection()).thenReturn(mockConnection);

        // Re-initialize or directly call checkAndEstablishConnection if constructor doesn't re-trigger with new mocks
        // For this test, let's assume constructor call is enough or we make a direct call.
        // If MqConnectionService is already created by @InjectMocks, its constructor has run.
        // To test initial state *after* specific mock setup, we might need to call a method or re-initialize.
        // Let's ensure checkAndEstablishConnection is called in a controlled way post-mock setup for clarity
        
        // Resetting the service to simulate construction AFTER mocks are set for initial connection
        mqConnectionService = new MqConnectionService(mqConfig, connectionFactory, eventPublisher);
        // The constructor calls checkAndEstablishConnection()

        assertTrue(mqConnectionService.isConnected(), "Should be connected after successful initial attempt.");
        assertEquals(0, mqConnectionService.getCurrentReconnectAttempts(), "Reconnect attempts should be 0 on success.");
        assertNull(mqConnectionService.getPausedUntil(), "Should not be paused on successful connection.");
        
        // Verify ConnectionResumedEvent is NOT published on initial successful connection
        // (as per current logic, ResumedEvent is for recovery)
        verify(eventPublisher, never()).publishEvent(any(ConnectionResumedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(ConnectionPausedEvent.class));
    }

    // Test Case 2: Connection Failure and Retry Logic
    @Test
    void testConnectionFailureAndRetryLogic() throws JMSException {
        when(connectionFactory.createConnection()).thenThrow(new JMSException("Test connection failure"));
        
        // Constructor of MqConnectionService (via @InjectMocks or manual new) calls checkAndEstablishConnection once.
        // For this test, we'll assume it's already been called once by @InjectMocks setup.
        // Let's re-initialize for controlled test environment
        mqConnectionService = new MqConnectionService(mqConfig, connectionFactory, eventPublisher); // First attempt fails

        assertFalse(mqConnectionService.isConnected(), "Should not be connected after initial failure.");
        assertEquals(1, mqConnectionService.getCurrentReconnectAttempts(), "Reconnect attempts should be 1 after first failure.");

        // Simulate second attempt
        mqConnectionService.checkAndEstablishConnection();
        assertFalse(mqConnectionService.isConnected(), "Should still not be connected after second failure.");
        assertEquals(2, mqConnectionService.getCurrentReconnectAttempts(), "Reconnect attempts should be 2 after second failure.");
    }

    // Test Case 3: Reaching Max Reconnect Attempts and Pause
    @Test
    void testReachingMaxReconnectAttemptsAndPause() throws JMSException {
        when(mqConfig.getMaxReconnectAttempts()).thenReturn(2); // Override for this test
        when(connectionFactory.createConnection()).thenThrow(new JMSException("Test connection failure"));

        // Re-initialize with new mqConfig settings reflected
        mqConnectionService = new MqConnectionService(mqConfig, connectionFactory, eventPublisher); // Attempt 1

        assertFalse(mqConnectionService.isConnected());
        assertEquals(1, mqConnectionService.getCurrentReconnectAttempts());

        mqConnectionService.checkAndEstablishConnection(); // Attempt 2

        assertFalse(mqConnectionService.isConnected());
        assertEquals(2, mqConnectionService.getCurrentReconnectAttempts()); // Reached max attempts

        // Next call should trigger pause
        mqConnectionService.checkAndEstablishConnection(); 

        assertNotNull(mqConnectionService.getPausedUntil(), "Should be paused after reaching max attempts.");
        assertTrue(mqConnectionService.getPausedUntil().isAfter(LocalDateTime.now().minusSeconds(10)), "Pause time should be in the future.");
        
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof ConnectionPausedEvent, "ConnectionPausedEvent should be published.");
        ConnectionPausedEvent pausedEvent = (ConnectionPausedEvent) eventCaptor.getValue();
        assertNotNull(pausedEvent.getPausedUntil());
    }

    // Test Case 4: Connection Success After Failures and Pause
    @Test
    void testConnectionSuccessAfterPause() throws JMSException {
        when(mqConfig.getMaxReconnectAttempts()).thenReturn(1); // Max 1 attempt before pause
        when(mqConfig.getReconnectPauseMinutes()).thenReturn(-1); // Pause for a negative minute (i.e., pause has expired)

        // First attempt fails
        when(connectionFactory.createConnection()).thenThrow(new JMSException("Initial failure"));
        mqConnectionService = new MqConnectionService(mqConfig, connectionFactory, eventPublisher); // Attempt 1 fails, pause triggered

        assertFalse(mqConnectionService.isConnected());
        assertEquals(1, mqConnectionService.getCurrentReconnectAttempts());
        assertNotNull(mqConnectionService.getPausedUntil(), "Service should be paused.");
        verify(eventPublisher).publishEvent(any(ConnectionPausedEvent.class)); // Pause event

        // Now, simulate pause has ended and next attempt will succeed
        // MqConfig already set to make pause effectively expired.
        // Reset connectionFactory mock for successful connection
        reset(connectionFactory); // Reset the whole mock
        when(connectionFactory.createConnection()).thenReturn(mockConnection); // Next attempt succeeds

        mqConnectionService.checkAndEstablishConnection(); // This call should find pause ended and attempt to connect

        assertTrue(mqConnectionService.isConnected(), "Should be connected after pause and successful retry.");
        assertEquals(0, mqConnectionService.getCurrentReconnectAttempts(), "Attempts should reset after successful connection.");
        assertNull(mqConnectionService.getPausedUntil(), "Pause should be cleared after successful connection.");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof ConnectionResumedEvent, "ConnectionResumedEvent should be published.");
    }
    
    // Test Case 5: Manual Trigger
    @Test
    void testManualTrigger_NotPaused_Connects() throws JMSException {
        // Ensure not connected initially, but not paused
        when(connectionFactory.createConnection()).thenThrow(new JMSException("Initial failure for setup"));
        mqConnectionService = new MqConnectionService(mqConfig, connectionFactory, eventPublisher);
        assertEquals(1, mqConnectionService.getCurrentReconnectAttempts());
        assertFalse(mqConnectionService.isConnected());
        assertNull(mqConnectionService.getPausedUntil()); // Not paused

        // Setup for successful connection on manual trigger
        reset(connectionFactory); // Clear previous stubbing
        when(connectionFactory.createConnection()).thenReturn(mockConnection);

        mqConnectionService.triggerManualReconnect();

        assertTrue(mqConnectionService.isConnected(), "Should connect on manual trigger when not paused.");
        assertEquals(0, mqConnectionService.getCurrentReconnectAttempts(), "Attempts should reset on manual trigger if not paused.");
        verify(eventPublisher, times(1)).publishEvent(any(ConnectionResumedEvent.class)); // Assuming it was a recovery
    }

    @Test
    void testManualTrigger_WhenPaused_ShouldNotConnectIfPauseNotExpired() throws JMSException {
        when(mqConfig.getMaxReconnectAttempts()).thenReturn(1);
        when(mqConfig.getReconnectPauseMinutes()).thenReturn(5); // Pause for 5 minutes

        when(connectionFactory.createConnection()).thenThrow(new JMSException("Failure"));
        mqConnectionService = new MqConnectionService(mqConfig, connectionFactory, eventPublisher); // Fails and pauses

        assertNotNull(mqConnectionService.getPausedUntil());
        assertTrue(mqConnectionService.getPausedUntil().isAfter(LocalDateTime.now()));
        verify(eventPublisher).publishEvent(any(ConnectionPausedEvent.class));

        // Try to trigger manually while still paused
        mqConnectionService.triggerManualReconnect();

        assertFalse(mqConnectionService.isConnected(), "Should not connect if manual trigger is during active pause.");
        // No ConnectionResumedEvent should be published
        verify(eventPublisher, never()).publishEvent(any(ConnectionResumedEvent.class));
    }
}
