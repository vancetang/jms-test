package com.vance.jms.service;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
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
 * 使用 JmsLifecycleManagerService 實現 MQ 斷線時的重新連線機制
 */
@Slf4j
@Service
public class MessageReceiver {

    @Autowired
    private MqConnectionService mqConnectionService;

    /**
     * 單一監聽器方法，根據訊息類型分派處理
     *
     * @param message 接收到的原始 JMS 訊息
     */
    @JmsListener(destination = "${mq-config.queue-name}", containerFactory = "jmsListenerContainerFactory", id = "mainMessageListener")
    public void onMessage(Message message) {
        log.info("收到訊息: {}", message);
        try {
            // 檢查 MQ 連接狀態
            if (!mqConnectionService.isConnected()) {
                log.warn("MQ 連接已中斷，無法處理訊息。訊息將被放回隊列或丟棄。");
                throw new JMSException("MQ 連接已中斷");
            }

            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                log.info("接收到文本訊息: {}", text);
                handleTextMessage(text);
            } else if (message instanceof ObjectMessage) {
                Serializable object = ((ObjectMessage) message).getObject();
                log.info("接收到物件訊息: {}", object);
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
                log.info("接收到二進制訊息: {} bytes", bytes.length);
                handleByteMessage(bytes);
            } else {
                log.warn("接收到未知訊息類型: {}", message.getClass().getName());
                // 處理未知訊息類型
            }
        } catch (JMSException e) {
            log.error("處理 JMS 訊息時發生錯誤: {}", e.getMessage(), e);
            // 當 MQ 連接中斷時，JmsLifecycleManagerService 會停止監聽器，
            // 所以這裡不需要額外處理
        } catch (Exception e) {
            log.error("處理訊息時發生未知錯誤: {}", e.getMessage(), e);
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
