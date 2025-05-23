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
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import lombok.extern.slf4j.Slf4j;

/**
 * JMS 配置類
 */
@Slf4j
@Configuration
@EnableJms
public class JmsConfig {

    @Autowired
    MqConfig mqConfig;

    /**
     * 配置 JMS 監聽器容器工廠
     * 設定為自動啟動，但由 JmsLifecycleManagerService 控制啟動和停止
     * 禁用 DefaultMessageListenerContainer 的默認重試機制，使用自定義的 MQ 重連機制
     */
    @Bean
    JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter());

        // 設定為自動啟動，但由 JmsLifecycleManagerService 控制啟動和停止
        factory.setAutoStartup(true);

        // 設定錯誤處理策略
        factory.setErrorHandler(t -> {
            log.error("JMS 監聽器錯誤處理: {}, {}", t.getMessage(), t);
        });

        // 設定並發消費者數量
        factory.setConcurrency("1-1");

        // 設定接收超時
        factory.setReceiveTimeout(1000L);

        // 設定會話事務
        factory.setSessionTransacted(true);

        // 設定訂閱持久性
        factory.setSubscriptionDurable(false);

        // 禁用 DefaultMessageListenerContainer 的默認重試機制
        // 設置為 0 表示不進行重試，而是立即失敗
        // 這樣可以確保使用我們自定義的 MQ 重連機制
        // factory.setRecoveryInterval(0L);
        BackOff backOff = new BackOff() {
            @Override
            public BackOffExecution start() {
                return new BackOffExecution() {
                    @Override
                    public long nextBackOff() {
                        return BackOffExecution.STOP;
                    }
                };
            }
        };
        factory.setBackOff(backOff);

        log.info("已配置 JMS 監聽器容器工廠，禁用默認重試機制，使用自定義 MQ 重連機制");

        return factory;
    }

    /**
     * 配置 JmsTemplate，設定訊息過期時間
     */
    @Bean
    JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
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
    MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
