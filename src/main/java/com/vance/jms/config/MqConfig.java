package com.vance.jms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "mq-config")
public class MqConfig {
    /**
     * 要連接的隊列名稱
     */
    private String queueName;

    /**
     * 訊息過期時間 (秒)
     */
    private int messageTtlSeconds;
}
