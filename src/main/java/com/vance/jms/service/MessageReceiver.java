package com.vance.jms.service;

import org.springframework.jms.annotation.JmsListener;

import com.vance.jms.model.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * 訊息接收服務
 */
@Slf4j
// @Service
public class MessageReceiver {

    /**
     * 監聽並接收 Message 物件訊息
     *
     * @param message 接收到的訊息
     */
    @JmsListener(destination = "${mq-config.queue-name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveMessage(Message message) {
        log.info("接收到訊息物件: {}", message);
        // 在這裡處理接收到的訊息
        // 例如：保存到資料庫、觸發其他業務邏輯等
    }

    /**
     * 監聽並接收文本訊息
     * 注意：如果同時定義了多個監聽器，Spring 會根據訊息類型選擇合適的監聽器
     *
     * @param text 接收到的文本
     */
    @JmsListener(destination = "${mq-config.queue-name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveTextMessage(String text) {
        log.info("接收到文本訊息: {}", text);
        // 在這裡處理接收到的文本訊息
    }

    /**
     * 監聽並接收二進制數據訊息
     * 注意：如果同時定義了多個監聽器，Spring 會根據訊息類型選擇合適的監聽器
     *
     * @param bytes 接收到的二進制數據
     */
    @JmsListener(destination = "${mq-config.queue-name}", containerFactory = "jmsListenerContainerFactory")
    public void receiveByteMessage(byte[] bytes) {
        log.info("接收到二進制數據訊息: {} bytes", bytes.length);
        // 在這裡處理接收到的二進制數據
        // 例如：保存到文件、進行數據處理等
    }
}
