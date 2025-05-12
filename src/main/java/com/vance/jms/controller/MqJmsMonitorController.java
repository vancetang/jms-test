package com.vance.jms.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vance.jms.service.MqJmsMonitorService;

import lombok.extern.slf4j.Slf4j;

/**
 * MQ JMS 直接監控控制器
 * 提供基於 JMS API 的 MQ 監控功能，直接獲取數據，不使用緩存
 */
@Slf4j
@RestController
@RequestMapping("/api/mq-monitor")
public class MqJmsMonitorController {

    @Autowired
    private MqJmsMonitorService mqJmsMonitorService;

    /**
     * 獲取 Queue Manager 信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getQueueManagerInfo() {
        log.info("獲取 Queue Manager 信息");
        Map<String, Object> info = mqJmsMonitorService.getQueueManagerInfo();
        return ResponseEntity.ok(info);
    }

    /**
     * 獲取隊列深度
     */
    @GetMapping("/queues/{queueName}/depth")
    public ResponseEntity<Map<String, Object>> getQueueDepth(@PathVariable String queueName) {
        log.info("獲取隊列深度: {}", queueName);
        long startTime = System.currentTimeMillis();
        int depth = mqJmsMonitorService.getQueueDepth(queueName);
        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("queueName", queueName);
        result.put("depth", depth);
        result.put("executionTimeMs", endTime - startTime);

        return ResponseEntity.ok(result);
    }

    /**
     * 獲取所有隊列深度
     */
    @GetMapping("/queues/depths")
    public ResponseEntity<Map<String, Object>> getAllQueueDepths() {
        log.info("獲取所有隊列深度");
        Map<String, Object> result = mqJmsMonitorService.getAllQueueDepths();
        return ResponseEntity.ok(result);
    }

    /**
     * 添加隊列到監控列表
     */
    @PostMapping("/queues/monitor")
    public ResponseEntity<Map<String, Object>> addQueueToMonitor(@RequestParam String queueName) {
        log.info("添加隊列到監控列表: {}", queueName);
        mqJmsMonitorService.addQueueToMonitor(queueName);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "隊列 " + queueName + " 已添加到監控列表");

        return ResponseEntity.ok(result);
    }

    /**
     * 測試 MQ 連接
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        log.info("測試 MQ 連接");
        Map<String, Object> result = mqJmsMonitorService.testConnection();
        return ResponseEntity.ok(result);
    }
}
