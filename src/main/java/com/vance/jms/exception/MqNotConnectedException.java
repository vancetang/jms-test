package com.vance.jms.exception;

/**
 * MQ 未連接異常
 * 當嘗試在 MQ 連接不可用時發送訊息時拋出此異常
 */
public class MqNotConnectedException extends RuntimeException {
    /**
     * 使用指定的錯誤訊息建立一個新的 MQ 未連接異常
     *
     * @param message 錯誤訊息
     */
    public MqNotConnectedException(String message) {
        super(message);
    }

    /**
     * 使用指定的錯誤訊息和原因建立一個新的 MQ 未連接異常
     *
     * @param message 錯誤訊息
     * @param cause   原因
     */
    public MqNotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
