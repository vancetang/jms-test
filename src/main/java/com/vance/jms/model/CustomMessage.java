package com.vance.jms.model;

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
public class CustomMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String content;
    private long timestamp;

    /**
     * 建立一個新的 CustomMessage 物件
     *
     * @param id
     * @param content
     * @return
     */
    public static CustomMessage of(String id, String content) {
        return new CustomMessage(id, content, System.currentTimeMillis());
    }
}
