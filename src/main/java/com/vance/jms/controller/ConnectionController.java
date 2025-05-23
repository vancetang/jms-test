package com.vance.jms.controller;

import com.vance.jms.service.MqConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing and viewing MQ connection status.
 */
@RestController
@RequestMapping("/api/mq")
public class ConnectionController {

    private final MqConnectionService mqConnectionService;

    @Autowired
    public ConnectionController(MqConnectionService mqConnectionService) {
        this.mqConnectionService = mqConnectionService;
    }

    /**
     * Triggers a manual reconnection attempt to the MQ server.
     *
     * @return A response entity indicating the outcome of the trigger.
     */
    @PostMapping("/reconnect/trigger")
    public ResponseEntity<Map<String, Object>> triggerReconnect() {
        mqConnectionService.triggerManualReconnect();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Manual reconnection process triggered.");
        // Note: The actual connection result is asynchronous. 
        // The status endpoint should be used to check current state.
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current status of the MQ connection.
     *
     * @return A response entity with connection status details.
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
