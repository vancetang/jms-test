package com.vance.jms.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // 引入 HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vance.jms.exception.MqNotConnectedException; // 引入 MQ 未連接異常
import com.vance.jms.model.CustomMessage;
import com.vance.jms.service.MessageSender;

import lombok.extern.slf4j.Slf4j;

/**
 * 訊息控制器，提供 API 端點來發送訊息
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    MessageSender messageSender;

    /**
     * 發送物件訊息
     *
     * @param message 要發送的訊息
     * @return 操作結果
     */
    @PostMapping("send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody CustomMessage message) {
        log.info("收到發送訊息請求: {}", message);

        // 如果沒有提供 ID，則生成一個
        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }

        // 設置時間戳
        message.setTimestamp(System.currentTimeMillis());

        try {
            // 發送訊息
            messageSender.sendMessage(message);

            // 返回結果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "訊息已成功發送");
            response.put("messageId", message.getId());
            return ResponseEntity.ok(response);
        } catch (MqNotConnectedException e) {
            log.error("由於 MQ 連接問題，無法發送自定義訊息: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "MQ 服務目前不可用。請稍後再試。");
            errorResponse.put("errorDetail", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            log.error("發送自定義訊息時發生意外錯誤: {}", message, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "處理您的請求時發生意外錯誤。");
            // 避免直接暴露通用異常的 e.getMessage()，因為它可能包含敏感信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 發送文本訊息
     *
     * @param payload 包含文本內容的請求體
     * @return 操作結果
     */
    @PostMapping("send-text")
    public ResponseEntity<Map<String, Object>> sendTextMessage(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        log.info("收到發送文本訊息請求: {}", text);

        try {
            // 發送文本訊息
            messageSender.sendTextMessage(text);

            // 返回結果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文本訊息已成功發送");
            return ResponseEntity.ok(response);
        } catch (MqNotConnectedException e) {
            log.error("由於 MQ 連接問題，無法發送文本訊息: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "MQ 服務目前不可用。請稍後再試。");
            errorResponse.put("errorDetail", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            log.error("發送文本訊息時發生意外錯誤: {}", text, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "處理您的請求時發生意外錯誤。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 發送二進制數據訊息
     * 接受 Base64 編碼的二進制數據
     *
     * @param payload 包含 Base64 編碼的二進制數據的請求體
     * @return 操作結果
     */
    @PostMapping("send-bytes")
    public ResponseEntity<Map<String, Object>> sendByteMessage(@RequestBody Map<String, String> payload) {
        String base64Data = payload.get("data");
        log.info("收到發送二進制數據請求: {} 字符的 Base64 數據", base64Data != null ? base64Data.length() : "null");

        if (base64Data == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "請求中缺少二進制訊息的 'data' 欄位。");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Data);
            log.info("解碼後的二進制數據大小: {} bytes", bytes.length);
        } catch (IllegalArgumentException e) {
            log.error("無法解碼 Base64 數據: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "提供的 Base64 數據無效。");
            errorResponse.put("errorDetail", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // 發送二進制數據
            messageSender.sendByteMessage(bytes);

            // 返回結果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "二進制數據已成功發送");
            response.put("byteLength", bytes.length);
            return ResponseEntity.ok(response);
        } catch (MqNotConnectedException e) {
            log.error("由於 MQ 連接問題，無法發送二進制訊息: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "MQ 服務目前不可用。請稍後再試。");
            errorResponse.put("errorDetail", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            log.error("發送二進制訊息時發生意外錯誤 (解碼後長度: {} bytes)",
                    bytes.length, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "處理您的請求時發生意外錯誤。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
