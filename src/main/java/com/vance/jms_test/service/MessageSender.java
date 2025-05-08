package com.vance.jms_test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.vance.jms_test.constant.Constant;
import com.vance.jms_test.model.Message;

import lombok.extern.slf4j.Slf4j;

/**
 * 訊息發送服務
 */
@Slf4j
@Service
public class MessageSender {

    @Autowired
    JmsTemplate jmsTemplate;

    private static final String QUEUE_NAME = Constant.Queue.DEFAULT_QUEUE;

    /**
     * 發送訊息到指定隊列，訊息將在 10 秒後自動過期
     * (過期時間在 JmsTemplate 中全局設定)
     *
     * @param message 要發送的訊息
     */
    public void sendMessage(Message message) {
        log.info("發送訊息到隊列 {}: {}", QUEUE_NAME, message);
        jmsTemplate.convertAndSend(QUEUE_NAME, message);
        log.info("訊息已成功發送，將在 10 秒後過期");
    }

    /**
     * 發送文本訊息到指定隊列，訊息將在 10 秒後自動過期
     * (過期時間在 JmsTemplate 中全局設定)
     *
     * @param text 要發送的文本
     */
    public void sendTextMessage(String text) {
        log.info("發送文本訊息到隊列 {}: {}", QUEUE_NAME, text);
        jmsTemplate.convertAndSend(QUEUE_NAME, text);
        log.info("文本訊息已成功發送，將在 10 秒後過期");
    }
}
