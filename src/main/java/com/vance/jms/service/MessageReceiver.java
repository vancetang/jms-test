package com.vance.jms.service;

import java.io.Serializable;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import com.vance.jms.model.CustomMessage;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 訊息接收服務
 */
@Slf4j
@Service
public class MessageReceiver {

    /**
     * 單一監聽器方法，根據訊息類型分派處理
     *
     * @param message 接收到的原始 JMS 訊息
     */
    @JmsListener(destination = "${mq-config.queue-name}", containerFactory = "jmsListenerContainerFactory")
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                handleTextMessage(text);
            } else if (message instanceof ObjectMessage) {
                Serializable object = ((ObjectMessage) message).getObject();
                if (object instanceof CustomMessage) {
                    handleObjectMessage((CustomMessage) object);
                } else {
                    log.warn("接收到未知類型的 ObjectMessage: {}", object.getClass().getName());
                    // 處理未知類型的 ObjectMessage
                }
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(bytes);
                handleByteMessage(bytes);
            } else {
                log.warn("接收到未知訊息類型: {}", message.getClass().getName());
                // 處理未知訊息類型
            }
        } catch (JMSException e) {
            log.error("處理 JMS 訊息時發生錯誤", e);
            // 處理 JMS 異常
        } catch (Exception e) {
            log.error("處理訊息時發生未知錯誤", e);
            // 處理其他異常
        }
    }

    /**
     * 處理接收到的 Message 物件訊息
     *
     * @param message 接收到的訊息物件
     */
    private void handleObjectMessage(CustomMessage message) {
        log.info("處理訊息物件: {}", message);
        // 在這裡添加處理 Message 物件的邏輯
    }

    /**
     * 處理接收到的文本訊息
     *
     * @param text 接收到的文本
     */
    private void handleTextMessage(String text) {
        log.info("處理文本訊息: {}", text);
        // 在這裡添加處理文本訊息的邏輯
    }

    /**
     * 處理接收到的二進制數據訊息
     *
     * @param bytes 接收到的二進制數據
     */
    private void handleByteMessage(byte[] bytes) {
        log.info("處理二進制數據訊息: {} bytes", bytes.length);
        // 在這裡添加處理二進制數據的邏輯
    }
}
