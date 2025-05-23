package com.vance.jms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for IBM MQ connection and behavior.
 * This class holds settings such as the target queue name, message time-to-live
 * (TTL),
 * and parameters for reconnection logic including interval, max attempts, and
 * pause duration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "mq-config")
public class MqConfig {
    /**
     * The name of the queue to connect to.
     * 對應中文：要連接的隊列名稱
     */
    private String queueName;

    /**
     * 訊息過期時間 (秒)
     */
    private int messageTtlSeconds;

    /**
     * 重新連接嘗試之間的間隔（秒）。默認為 30 秒。
     */
    private int reconnectIntervalSeconds = 30;

    /**
     * 最大重新連接嘗試次數。默認為 30 次。
     */
    private int maxReconnectAttempts = 30;

    /**
     * 達到最大重新連接嘗試次數後，再次嘗試之前的暫停持續時間（分鐘）。默認為 30 分鐘。
     */
    private int reconnectPauseMinutes = 30;
}
