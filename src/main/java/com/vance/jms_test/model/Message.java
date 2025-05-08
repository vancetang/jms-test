package com.vance.jms_test.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 訊息模型類，用於 JMS 訊息傳輸
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String content;
    private long timestamp;
    
    // 建立一個帶有當前時間戳的新訊息
    public static Message of(String id, String content) {
        return new Message(id, content, System.currentTimeMillis());
    }
}
