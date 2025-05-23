package com.vance.jms.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import com.vance.jms.config.MqConfig;
import com.vance.jms.exception.MqNotConnectedException;
import com.vance.jms.model.CustomMessage;

@ExtendWith(MockitoExtension.class)
public class MessageSenderTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MqConfig mqConfig;

    @Mock
    private MqConnectionService mqConnectionService;

    @InjectMocks
    private MessageSender messageSender;

    private final String TEST_QUEUE_NAME = "TEST.QUEUE";

    // Test Case 1.1: Send CustomMessage When Connected
    @Test
    void testSendCustomMessage_WhenConnected_ShouldSend() {
        when(mqConnectionService.isConnected()).thenReturn(true);
        when(mqConfig.getQueueName()).thenReturn(TEST_QUEUE_NAME);
        CustomMessage message = new CustomMessage("id1", "content", System.currentTimeMillis());

        assertDoesNotThrow(() -> messageSender.sendMessage(message));

        verify(jmsTemplate, times(1)).convertAndSend(TEST_QUEUE_NAME, message);
    }

    // Test Case 1.2: Send TextMessage When Connected
    @Test
    void testSendTextMessage_WhenConnected_ShouldSend() {
        when(mqConnectionService.isConnected()).thenReturn(true);
        when(mqConfig.getQueueName()).thenReturn(TEST_QUEUE_NAME);
        String textMessage = "Hello World";

        assertDoesNotThrow(() -> messageSender.sendTextMessage(textMessage));

        verify(jmsTemplate, times(1)).convertAndSend(TEST_QUEUE_NAME, textMessage);
    }

    // Test Case 1.3: Send ByteMessage When Connected
    @Test
    void testSendByteMessage_WhenConnected_ShouldSend() {
        when(mqConnectionService.isConnected()).thenReturn(true);
        when(mqConfig.getQueueName()).thenReturn(TEST_QUEUE_NAME);
        byte[] bytesMessage = new byte[] { 0, 1, 2 };

        assertDoesNotThrow(() -> messageSender.sendByteMessage(bytesMessage));

        verify(jmsTemplate, times(1)).convertAndSend(TEST_QUEUE_NAME, bytesMessage);
    }

    // Test Case 2.1: Send CustomMessage When Not Connected
    @Test
    void testSendCustomMessage_WhenNotConnected_ShouldThrowException() {
        when(mqConnectionService.isConnected()).thenReturn(false);
        CustomMessage message = new CustomMessage("id1", "content", System.currentTimeMillis());

        assertThrows(MqNotConnectedException.class, () -> {
            messageSender.sendMessage(message);
        });
    }

    // Test Case 2.2: Send TextMessage When Not Connected
    @Test
    void testSendTextMessage_WhenNotConnected_ShouldThrowException() {
        when(mqConnectionService.isConnected()).thenReturn(false);
        String textMessage = "Hello World";

        assertThrows(MqNotConnectedException.class, () -> {
            messageSender.sendTextMessage(textMessage);
        });

        verify(jmsTemplate, never()).convertAndSend(anyString(), (String) any());
    }

    // Test Case 2.3: Send ByteMessage When Not Connected
    @Test
    void testSendByteMessage_WhenNotConnected_ShouldThrowException() {
        when(mqConnectionService.isConnected()).thenReturn(false);
        byte[] bytesMessage = new byte[] { 0, 1, 2 };

        assertThrows(MqNotConnectedException.class, () -> {
            messageSender.sendByteMessage(bytesMessage);
        });

        verify(jmsTemplate, never()).convertAndSend(anyString(), any(byte[].class));
    }
}
