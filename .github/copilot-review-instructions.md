# Copilot Review Instructions (Java Spring Boot)

請根據以下指南生成程式碼審查建議，確保評論具體、可操作，並符合 Java Spring Boot 專案的最佳實踐。

## General Response Language
- **IMPORTANT:** Please provide all responses, comments, and suggestions exclusively in **Traditional Chinese (繁體中文)**.

## 總體原則
- **目標**: 提升程式碼品質、可維護性，並確保符合 Spring Boot 慣例。
- **語氣**: 專業、建設性，避免模糊或過於批評的評論。
- **具體性**: 指出問題位置（檔案、行號）並提供改進建議或範例。

## 審查重點
### 1. 程式碼正確性
- 檢查邏輯錯誤、邊界條件或潛在 bug。
- 確保功能符合規格（例如 REST API 的行為與文件一致）。
- 範例評論: "在 `UserService.java:50`，未檢查 `userId` 是否為 null，可能導致 NPE，建議添加 null 檢查。"

### 2. Spring Boot 特定規範
- **依賴注入**:
  - 優先使用構造函數注入而非 `@Autowired` 字段注入。
  - 範例評論: "建議將 `UserRepository` 從字段注入改為構造函數注入，提升測試性。"
- **控制器 (Controller)**:
  - 確保 REST API 使用適當的 HTTP 方法（GET、POST 等）和狀態碼。
  - 檢查是否使用 `@RestController` 和適當的 `@RequestMapping`。
  - 範例評論: "在 `UserController.java:20`，應使用 201 狀態碼表示資源創建成功。"
- **服務層 (Service)**:
  - 業務邏輯應放在 `@Service` 類中，避免控制器過於臃腫。
  - 範例評論: "建議將 `UserController.java:35` 的業務邏輯移動到 `UserService`。"
- **資料存取 (Repository)**:
  - 使用 Spring Data JPA 的 `@Repository` 並檢查查詢方法的正確性。
  - 避免直接使用 JDBC，除非有特殊需求。
  - 範例評論: "在 `UserRepository.java:15`，查詢應使用 `Optional` 處理空結果。"

### 3. Java 程式碼風格
- 遵循 Java Naming Conventions（類名大寫駝峰、方法名小寫駝峰）。
- 檢查是否符合專案的格式化工具（例如 Checkstyle 或 Spotless）。
- 範例評論: "建議將 `user_data` 改為 `userData` 以符合 Java 命名慣例。"

### 4. 效能
- 檢查是否有 N+1 查詢問題（特別是 JPA/Hibernate）。
- 避免在迴圈中呼叫資料庫或外部 API。
- 範例評論: "在 `OrderService.java:40`，建議使用 `fetch join` 解決 N+1 查詢問題。"

### 5. 安全性
- 檢查是否有未驗證的輸入（例如 REST 請求參數）。
- 確保敏感資料（例如密碼）使用加密（例如 Spring Security 的 `PasswordEncoder`）。
- 範例評論: "在 `AuthController.java:25`，建議使用 `@Valid` 驗證輸入參數。"

### 6. 測試覆蓋
- 確保新功能有單元測試（使用 JUnit 和 Mockito）。
- 檢查是否有整合測試（例如 `@SpringBootTest`）。
- 範例評論: "此變更缺少測試，建議為 `UserService.createUser()` 添加單元測試。"

### 7. 文件
- 檢查是否有足夠的 Javadoc（特別是公共方法和類）。
- 確保 REST API 有 OpenAPI/Swagger 文件。
- 範例評論: "建議在 `UserController.getUser()` 上添加 Javadoc，說明參數和返回值。"

### 8. 配置管理
- 檢查是否正確使用 `application.properties` 或 `application.yml`。
- 避免硬編碼配置（例如 URL、密鑰）。
- 範例評論: "在 `ApiClient.java:10`，建議將 API URL 移到 `application.yml` 中。"

## 團隊特定規範
- 使用 Spring Boot 3.x 和 Java 17（或根據專案指定版本）。
- 所有 REST API 必須遵循 RESTful 原則（例如無狀態、資源導向）。
- 提交前必須通過 `mvn test` 和 `mvn checkstyle:check`。

## 評論範例
檔案: src/main/java/com/example/UserController.java  
行號: 30  
問題: findUserById 未處理空結果，可能拋出 NPE。  
建議: 使用 Optional 並返回 404，例如：  
```java
public ResponseEntity<User> findUserById(@PathVariable Long id) {
    return userService.findById(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
}
```

檔案: src/main/java/com/example/UserService.java  
行號: 15  
問題: 使用 @Autowired 字段注入，不利於測試。  
建議: 改為構造函數注入，例如：  
```java
private final UserRepository userRepository;

public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```


## 注意事項
- 如果不確定問題，提出疑問而非假設（例如 "這裡是否需要事務管理？"）。
- 避免重複人類審查者已提出的建議。
- 若變更符合規範，可簡單肯定（例如 "程式碼結構良好，符合 Spring 慣例！"）。