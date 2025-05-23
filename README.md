# JMS 測試應用程式

這是一個基於 Spring Boot 的 JMS (Java Message Service) 測試應用程式，使用 IBM MQ 作為訊息中介軟體，實現了訊息的發送和接收功能，並具備 MQ 連線中斷時的重新連線機制。

## 功能特點

- 支援多種訊息類型的發送和接收：
  - 自定義物件訊息 (CustomMessage)
  - 文本訊息 (TextMessage)
  - 二進制訊息 (BytesMessage)
- 完整的 MQ 連線管理：
  - 自動重新連線機制
  - 連線狀態監控
  - 手動觸發重新連線
- RESTful API 介面：
  - 訊息發送 API
  - 連線狀態查詢和管理 API
- 斷線重連機制：
  - MessageSender 發送訊息前檢查連線狀態
  - MessageReceiver 接收訊息時檢查連線狀態
  - JmsLifecycleManagerService 管理 JMS 監聽器生命週期

## 技術架構

- **Spring Boot 3.5.0**：應用程式框架
- **IBM MQ JMS Spring Boot Starter**：IBM MQ 的 Spring Boot 整合
- **Spring Web**：RESTful API 支援
- **Spring Actuator**：應用程式監控
- **Lombok**：簡化 Java 程式碼
- **Springdoc OpenAPI**：API 文檔生成

## 系統需求

- Java 21 或更高版本
- Maven 3.8+ (或使用包含的 Maven Wrapper)
- IBM MQ 伺服器 (本地或遠端)

## 快速開始

### 1. 配置 IBM MQ 連接

在 `src/main/resources/application.yml` 中配置您的 IBM MQ 連接參數：

#### IBM MQ 連接參數 (由 `ibm-mq-spring-boot-starter` 提供)
```yaml
ibm:
  mq:
    queue-manager: MQJ006D      # MQ Queue Manager 名稱
    channel: DEV.ADMIN.SVRCONN  # MQ Channel 名稱
    conn-name: localhost(3434)  # MQ 連接位址和端口，例如：host(port)
    user: mqm                   # MQ 連接使用者名稱 (如果 MQ 伺服器有安全設置)
    # password: yourpassword    # MQ 連接密碼 (如果 MQ 伺服器有安全設置)
```

#### 應用程式特定 MQ 配置 (本專案 `MqConfig.java` 使用)
```yaml
mq-config:
  queueName: YOUR_TARGET_QUEUE_NAME # 要發送/接收訊息的隊列名稱
  messageTtlSeconds: 10           # 預設訊息過期時間 (秒)
  reconnectIntervalSeconds: 30    # MQ 連接重試間隔 (秒)
  maxReconnectAttempts: 30        # 在暫停重連之前的最大嘗試次數
  reconnectPauseMinutes: 30       # 最大嘗試次數後的暫停持續時間 (分鐘)
```
**新增配置說明:**
- `reconnectIntervalSeconds`: 應用程式嘗試重新連接到 MQ 伺服器的間隔時間（秒）。
- `maxReconnectAttempts`: 在連接失敗達到此次數後，應用程式將暫停重連。
- `reconnectPauseMinutes`: 當達到最大重連嘗試次數後，應用程式將暫停指定的分鐘數，然後重新開始嘗試連接。

### 2. 啟動應用程式

使用 Maven 啟動應用程式：

```bash
./mvnw spring-boot:run
```

或使用 Java 直接執行：

```bash
./mvnw clean package
java -jar target/jms-test-0.0.1-SNAPSHOT.jar
```

### 3. MQ 斷線重連機制

本應用程式實現了完整的 MQ 斷線重連機制：

#### 連線狀態監控
- MqConnectionService 維護連線狀態，使用 AtomicBoolean 確保線程安全
- 定期檢查連線狀態並嘗試重新連線（每隔 `reconnectIntervalSeconds` 秒）
- 如果連續失敗達到 `maxReconnectAttempts` 次，重連機制將暫停 `reconnectPauseMinutes` 分鐘

#### 事件發布機制
- 連線暫停時發布 ConnectionPausedEvent 事件
- 連線恢復時發布 ConnectionResumedEvent 事件
- JmsLifecycleManagerService 監聽這些事件並管理 JMS 監聽器生命週期

#### JMS 監聽器生命週期管理
- JmsListenerContainerFactory 設定為不自動啟動
- 連線中斷時停止 JMS 監聽器，避免無效的消息處理
- 連線恢復時啟動 JMS 監聽器，恢復消息處理

#### 訊息發送與接收處理
- MessageSender 在發送訊息前檢查連線狀態，如果連線中斷則拋出 MqNotConnectedException
- MessageReceiver 在處理訊息前檢查連線狀態，如果連線中斷則拋出 JMSException
- 控制器層捕獲這些異常並返回適當的 HTTP 狀態碼和錯誤訊息

### 4. API 端點

#### 發送訊息

##### 發送物件訊息

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"content":"這是一條測試訊息"}'
```

##### 發送文本訊息

```bash
curl -X POST http://localhost:8080/api/messages/send-text \
  -H "Content-Type: application/json" \
  -d '{"text":"這是一條純文本測試訊息"}'
```

##### 發送二進制數據

```bash
curl -X POST http://localhost:8080/api/messages/send-bytes \
  -H "Content-Type: application/json" \
  -d '{"data":"SGVsbG8gV29ybGQh"}'
```

> 注意：二進制數據需要先進行 Base64 編碼。上面的例子中，"SGVsbG8gV29ybGQh" 是 "Hello World!" 的 Base64 編碼。

**訊息發送行為變更**:
如果 MQ 連接不可用（例如，在重連暫停期間或 MQ 伺服器確實無法訪問），上述發送訊息的 API 端點將返回 **HTTP 503 (Service Unavailable)** 錯誤，並附帶一個 JSON 回應體，說明問題。例如：
```json
{
    "success": false,
    "message": "MQ 服務目前不可用。請稍後再試。",
    "errorDetail": "無法發送訊息。MQ 未連接。"
}
```

#### MQ 連接管理 API

##### 手動觸發重連
此端點允許手動觸發一個新的 MQ 重連嘗試週期。如果當前處於暫停期，並且暫停時間未結束，則不會立即觸發，但會重置嘗試計數器（除非手動觸發時暫停期剛好結束）。
```bash
curl -X POST http://localhost:8080/api/mq/reconnect/trigger
```
回應示例:
```json
{
    "message": "已觸發手動重新連接程序。"
}
```

##### 獲取 MQ 連接狀態
此端點檢索當前 MQ 連接的狀態。
```bash
curl -X GET http://localhost:8080/api/mq/status
```
回應示例 (已連接):
```json
{
    "connected": true,
    "currentAttempts": 0,
    "pausedUntil": null
}
```
回應示例 (未連接且正在重試):
```json
{
    "connected": false,
    "currentAttempts": 5,
    "pausedUntil": null
}
```
回應示例 (未連接且處於暫停期):
```json
{
    "connected": false,
    "currentAttempts": 30, // 或上次達到 maxReconnectAttempts 的值
    "pausedUntil": "2023-10-27T15:30:00.123456" // 暫停結束的時間
}
```

## 檔案結構

```
src/main/java/com/vance/jms/
├── config/
│   ├── JmsConfig.java           # JMS 配置類
│   └── MqConfig.java            # MQ 配置類
├── controller/
│   ├── ConnectionController.java # MQ 連線管理 API 控制器
│   └── MessageController.java   # 訊息發送 API 控制器
├── event/
│   ├── ConnectionPausedEvent.java # MQ 連線暫停事件
│   └── ConnectionResumedEvent.java # MQ 連線恢復事件
├── exception/
│   └── MqNotConnectedException.java # MQ 未連線異常
├── model/
│   └── CustomMessage.java       # 自定義訊息模型
├── service/
│   ├── JmsLifecycleManagerService.java # JMS 生命週期管理服務
│   ├── MessageReceiver.java     # 訊息接收服務
│   ├── MessageSender.java       # 訊息發送服務
│   └── MqConnectionService.java # MQ 連線管理服務
└── JmsTestApplication.java      # 應用程式入口
```

## 重要類說明

- **JmsTestApplication**：應用程式入口點，啟用 JMS 和排程功能
- **MessageSender**：負責發送訊息，並在發送前檢查 MQ 連線狀態
- **MessageReceiver**：負責接收和處理訊息，並在處理前檢查 MQ 連線狀態
- **MqConnectionService**：管理 MQ 連線狀態，提供重新連線機制
- **JmsLifecycleManagerService**：根據 MQ 連線狀態管理 JMS 監聽器的生命週期

## 待辦事項與未來測試計劃

以下是未來可以進行的測試和功能擴展：

### 基礎功能測試

- [x] 測試不同類型訊息的發送和接收 (JSON、文本、二進制等)
- [x] **測試 MQ 連接中斷時的錯誤處理 (部分通過自動重連機制實現)**
- [ ] 測試其他格式訊息的發送和接收 (XML等)
- [ ] 測試大量訊息的批次處理能力
- [ ] 測試訊息的持久化和非持久化模式
- [ ] 驗證訊息過期機制在不同場景下的表現

### 性能測試

- [ ] 進行高並發訊息發送測試 (每秒處理訊息數)
- [ ] 測量訊息處理的延遲時間
- [ ] 評估系統在高負載下的穩定性
- [ ] 測試長時間運行下的記憶體使用情況

### 錯誤處理與恢復

- [x] **實現和測試訊息重試機制 (應用程式級別的 MQ 連接重試)**
- [ ] 測試訊息處理失敗時的死信隊列功能
- [ ] 開發和測試訊息處理的事務支援

### 安全性測試

- [ ] 實現和測試 MQ 連接的 SSL/TLS 加密
- [ ] 測試訊息內容加密功能
- [ ] 實現基於角色的訊息發送權限控制
- [ ] 審計日誌記錄與安全事件監控

### 功能擴展

- [ ] 實現訊息優先級設定功能
- [ ] 添加訊息確認回執機制
- [ ] 開發訊息路由功能 (基於內容或標頭)
- [ ] 實現訊息過濾器功能
- [ ] 添加訊息壓縮功能以提高效率
- [ ] 開發訊息轉換器以支援不同格式間的轉換

### 監控與管理

- [x] **實現 JMS 連接和隊列的監控指標 (部分通過 `/api/mq/status` 端點實現)**
- [ ] 整合 Spring Boot Actuator 提供更全面的健康檢查端點
- [ ] 開發管理控制台以查看訊息狀態
- [ ] 添加警報機制以通知異常情況

### 整合測試

- [ ] 與其他系統的整合測試 (如資料庫、緩存等)
- [ ] 測試在微服務架構中的訊息傳遞
- [ ] 實現和測試訊息驅動的事件處理流程
- [ ] 測試與其他消息中間件的互操作性

### 文檔與示例

- [x] **編寫詳細的 API 文檔 (已更新新端點)**
- [ ] 提供各種使用場景的示例代碼
- [ ] 建立性能測試報告模板
- [ ] 編寫部署指南和最佳實踐

## 貢獻指南

歡迎提交 Pull Request 或開 Issue 來改進這個專案。在提交代碼前，請確保：

1. 代碼符合專案的編碼風格
2. 添加了適當的單元測試
3. 所有測試都能通過
4. 更新了相關文檔

## 授權

本專案採用 [MIT 授權](LICENSE)。
