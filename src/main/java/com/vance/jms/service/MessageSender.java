package com.vance.jms.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.vance.jms.config.JmsConfig;
import com.vance.jms.model.CustomMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * 訊息發送服務
 */
@Slf4j
@Service
public class MessageSender {

    @Autowired
    JmsTemplate jmsTemplate;

    @Value("${mq-config.queue-name}")
    private String queueName;

    /**
     * 發送訊息到指定隊列，訊息將在 10 秒後自動過期
     * (過期時間在 JmsTemplate 中全局設定)
     *
     * @param message 要發送的訊息
     */
    public void sendMessage(CustomMessage message) {
        log.info("發送訊息到隊列 {}: {}", queueName, message);
        jmsTemplate.convertAndSend(queueName, message);
        log.info("訊息已成功發送，將在 {} 秒後過期", TimeUnit.MILLISECONDS.toSeconds(JmsConfig.MESSAGE_TTL));
    }

    /**
     * 發送文本訊息到指定隊列，訊息將在 10 秒後自動過期
     * (過期時間在 JmsTemplate 中全局設定)
     *
     * @param text 要發送的文本
     */
    public void sendTextMessage(String text) {
        log.info("發送文本訊息到隊列 {}: {}", queueName, text);
        jmsTemplate.convertAndSend(queueName, text);
        log.info("文本訊息已成功發送，將在 {} 秒後過期", TimeUnit.MILLISECONDS.toSeconds(JmsConfig.MESSAGE_TTL));
    }

    /**
     * 發送二進制數據到指定隊列，訊息將在 10 秒後自動過期
     * (過期時間在 JmsTemplate 中全局設定)
     *
     * @param bytes 要發送的二進制數據
     */
    public void sendByteMessage(byte[] bytes) {
        log.info("發送二進制數據到隊列 {}: {} bytes", queueName, bytes.length);
        jmsTemplate.convertAndSend(queueName, bytes);
        log.info("二進制數據已成功發送，將在 {} 秒後過期", TimeUnit.MILLISECONDS.toSeconds(JmsConfig.MESSAGE_TTL));
    }
}
