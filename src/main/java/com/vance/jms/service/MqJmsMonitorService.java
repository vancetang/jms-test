package com.vance.jms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.spring.boot.MQConfigurationProperties;

import jakarta.annotation.PostConstruct;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import lombok.extern.slf4j.Slf4j;

/**
 * IBM MQ 監控服務 (直接獲取版本)
 * 使用 JMS API 直接獲取 MQ 信息，不使用緩存
 */
@Slf4j
@Service
public class MqJmsMonitorService {

    @Autowired
    private MQConfigurationProperties mqProperties;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Value("${mq-config.queue-name:DEV.QUEUE.1}")
    private String defaultQueueName;

    // 隊列列表
    private List<String> queueNames = new ArrayList<>();

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        try {
            log.info("初始化 MQ JMS 直接監控服務");
            log.info("Queue Manager: {}", mqProperties.getQueueManager());
            log.info("Channel: {}", mqProperties.getChannel());
            log.info("Connection Name: {}", mqProperties.getConnName());
            log.info("User: {}", mqProperties.getUser());
            log.info("Default Queue: {}", defaultQueueName);

            // 添加默認隊列
            queueNames.add(defaultQueueName);

            // 添加其他常用隊列
            queueNames.add("DEV.QUEUE.2");
            queueNames.add("DEV.QUEUE.3");
            queueNames.add("DEV.DEAD.LETTER.QUEUE");
        } catch (Exception e) {
            log.error("初始化 MQ JMS 直接監控服務失敗: {}", e.getMessage(), e);
        }
    }

    /**
     * 獲取 Queue Manager 信息
     */
    public Map<String, Object> getQueueManagerInfo() {
        Map<String, Object> info = new HashMap<>();

        long startTime = System.currentTimeMillis();

        try {
            // 嘗試建立連接
            Connection connection = connectionFactory.createConnection();
            try {
                // 獲取連接元數據
                info.put("queueManagerName", mqProperties.getQueueManager());
                info.put("queueManagerStatus", "RUNNING");
                info.put("connected", true);

                // 獲取默認隊列深度
                int depth = getQueueDepth(defaultQueueName);
                info.put("defaultQueueName", defaultQueueName);
                info.put("defaultQueueDepth", depth);

                // 計算執行時間
                long endTime = System.currentTimeMillis();
                info.put("executionTimeMs", endTime - startTime);

                return info;
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("關閉連接失敗: {}", e.getMessage());
                }
            }
        } catch (JMSException e) {
            log.error("獲取 Queue Manager 信息失敗: {}", e.getMessage(), e);
            info.put("queueManagerName", mqProperties.getQueueManager());
            info.put("queueManagerStatus", "ERROR");
            info.put("error", e.getMessage());
            info.put("connected", false);

            // 計算執行時間
            long endTime = System.currentTimeMillis();
            info.put("executionTimeMs", endTime - startTime);

            return info;
        }
    }

    /**
     * 獲取隊列深度
     */
    public int getQueueDepth(String queueName) {
        long startTime = System.currentTimeMillis();

        try {
            // 使用 JMS API 獲取隊列深度
            Connection connection = connectionFactory.createConnection();
            try {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue(queueName);
                QueueBrowser browser = session.createBrowser(queue);

                // 計算消息數量
                int count = 0;
                for (java.util.Enumeration<?> e = browser.getEnumeration(); e.hasMoreElements();) {
                    e.nextElement();
                    count++;
                }

                browser.close();
                session.close();

                long endTime = System.currentTimeMillis();
                log.debug("獲取隊列 {} 深度耗時: {}ms", queueName, (endTime - startTime));

                return count;
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("關閉連接失敗: {}", e.getMessage());
                }
            }
        } catch (JMSException e) {
            log.error("獲取隊列 {} 深度失敗: {}", queueName, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 獲取所有隊列深度
     */
    public Map<String, Object> getAllQueueDepths() {
        long startTime = System.currentTimeMillis();

        Map<String, Integer> depths = new HashMap<>();
        for (String queueName : queueNames) {
            try {
                int depth = getQueueDepth(queueName);
                depths.put(queueName, depth);
            } catch (Exception e) {
                log.warn("獲取隊列 {} 深度失敗: {}", queueName, e.getMessage());
                depths.put(queueName, -1);
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        log.debug("獲取所有隊列深度耗時: {}ms", executionTime);

        Map<String, Object> result = new HashMap<>();
        result.put("depths", depths);
        result.put("executionTimeMs", executionTime);

        return result;
    }

    /**
     * 添加要監控的隊列
     */
    public void addQueueToMonitor(String queueName) {
        if (!queueNames.contains(queueName)) {
            queueNames.add(queueName);
            log.info("添加隊列 {} 到監控列表", queueName);
        }
    }

    /**
     * 測試 MQ 連接
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 嘗試建立連接
            Connection connection = connectionFactory.createConnection();
            try {
                connection.start();
                result.put("status", "success");
                result.put("message", "成功連接到 IBM MQ");
                result.put("queueManager", mqProperties.getQueueManager());
                result.put("connected", true);
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("關閉連接失敗: {}", e.getMessage());
                }
            }
        } catch (JMSException e) {
            log.error("測試 MQ 連接失敗: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "連接 IBM MQ 失敗: " + e.getMessage());
            result.put("connected", false);

            // 嘗試獲取更詳細的錯誤信息
            if (e.getLinkedException() instanceof MQException) {
                MQException mqe = (MQException) e.getLinkedException();
                result.put("reasonCode", mqe.reasonCode);
                result.put("reasonCodeExplained", getReasonCodeString(mqe.reasonCode));
            }
        }

        return result;
    }

    /**
     * 獲取原因代碼字符串
     */
    private String getReasonCodeString(int reasonCode) {
        switch (reasonCode) {
            case CMQC.MQRC_NONE:
                return "MQRC_NONE";
            case CMQC.MQRC_CONNECTION_BROKEN:
                return "MQRC_CONNECTION_BROKEN";
            case CMQC.MQRC_Q_MGR_NAME_ERROR:
                return "MQRC_Q_MGR_NAME_ERROR";
            case CMQC.MQRC_Q_MGR_NOT_AVAILABLE:
                return "MQRC_Q_MGR_NOT_AVAILABLE";
            case CMQC.MQRC_HOST_NOT_AVAILABLE:
                return "MQRC_HOST_NOT_AVAILABLE";
            case CMQC.MQRC_UNKNOWN_OBJECT_NAME:
                return "MQRC_UNKNOWN_OBJECT_NAME";
            case CMQC.MQRC_UNKNOWN_OBJECT_Q_MGR:
                return "MQRC_UNKNOWN_OBJECT_Q_MGR";
            case CMQC.MQRC_NOT_AUTHORIZED:
                return "MQRC_NOT_AUTHORIZED";
            case 2195: // MQRC_UNEXPECTED_ERROR
                return "MQRC_UNEXPECTED_ERROR";
            default:
                return "UNKNOWN_REASON_CODE (" + reasonCode + ")";
        }
    }
}
