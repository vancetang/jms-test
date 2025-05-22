package com.vance.jms.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;

/**
 * JMS 配置類
 */
@Configuration
@EnableJms
public class JmsConfig {

    @Autowired
    MqConfig mqConfig;

    /**
     * 配置 JMS 監聽器容器工廠
     */
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter());
        return factory;
    }

    /**
     * 配置 JmsTemplate，設定訊息過期時間
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setMessageConverter(jacksonJmsMessageConverter());

        // 啟用明確的 QoS 設定
        jmsTemplate.setExplicitQosEnabled(true);

        // 設定訊息過期時間
        jmsTemplate.setTimeToLive(TimeUnit.SECONDS.toMillis(mqConfig.getMessageTtlSeconds()));

        // 設定訊息傳遞模式 (PERSISTENT 或 NON_PERSISTENT)
        jmsTemplate.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        return jmsTemplate;
    }

    /**
     * 配置 Jackson 訊息轉換器，用於將 Java 物件轉換為 JMS 訊息
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
