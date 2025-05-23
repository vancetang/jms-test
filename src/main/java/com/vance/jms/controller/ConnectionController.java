package com.vance.jms.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vance.jms.service.MqConnectionService;

/**
 * MQ 連接狀態管理和查看的 REST 控制器
 */
@RestController
@RequestMapping("/api/mq")
public class ConnectionController {

    @Autowired
    private MqConnectionService mqConnectionService;

    /**
     * 觸發手動重新連接 MQ 伺服器的嘗試
     *
     * @return 包含觸發結果的回應實體
     */
    @PostMapping("/reconnect/trigger")
    public ResponseEntity<Map<String, Object>> triggerReconnect() {
        mqConnectionService.triggerManualReconnect();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "已觸發手動重新連接程序。");
        // 注意：實際連接結果是非同步的。
        // 應使用狀態端點檢查當前狀態。
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取 MQ 連接的當前狀態
     *
     * @return 包含連接狀態詳細信息的回應實體
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", mqConnectionService.isConnected());
        status.put("currentAttempts", mqConnectionService.getCurrentReconnectAttempts());
        LocalDateTime pausedUntil = mqConnectionService.getPausedUntil();
        status.put("pausedUntil", pausedUntil != null ? pausedUntil.toString() : null);

        return ResponseEntity.ok(status);
    }
}
