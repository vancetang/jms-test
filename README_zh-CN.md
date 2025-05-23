# JMS 测试应用程序

这是一个基于 Spring Boot 的 JMS (Java Message Service) 测试应用程序，使用 IBM MQ 作为消息中间件，实现了消息的发送和接收功能，并具备 MQ 连接中断时的重新连接机制。

## 功能特点

- 支持多种消息类型的发送和接收：
  - 自定义对象消息 (CustomMessage)
  - 文本消息 (TextMessage)
  - 二进制消息 (BytesMessage)
- 完整的 MQ 连接管理：
  - 自动重新连接机制
  - 连接状态监控
  - 手动触发重新连接
- RESTful API 接口：
  - 消息发送 API
  - 连接状态查询和管理 API
- 断线重连机制：
  - MessageSender 发送消息前检查连接状态
  - MessageReceiver 接收消息时检查连接状态
  - JmsLifecycleManagerService 管理 JMS 监听器生命周期

## 技术架构

- **Spring Boot 3.5.0**：应用程序框架
- **IBM MQ JMS Spring Boot Starter**：IBM MQ 的 Spring Boot 集成
- **Spring Web**：RESTful API 支持
- **Spring Actuator**：应用程序监控
- **Lombok**：简化 Java 代码
- **Springdoc OpenAPI**：API 文档生成

## 系统需求

- Java 21 或更高版本
- Maven 3.8+ (或使用包含的 Maven Wrapper)
- IBM MQ 服务器 (本地或远程)

## 快速开始

### 1. 配置 IBM MQ 连接

在 `src/main/resources/application.yml` 中配置您的 IBM MQ 连接参数：

#### IBM MQ 连接参数 (由 `ibm-mq-spring-boot-starter` 提供)
```yaml
ibm:
  mq:
    queue-manager: MQJ006D      # MQ 队列管理器名称
    channel: DEV.ADMIN.SVRCONN  # MQ 通道名称
    conn-name: localhost(3434)  # MQ 连接地址和端口，例如：host(port)
    user: mqm                   # MQ 连接用户名 (如果 MQ 服务器有安全设置)
    # password: yourpassword    # MQ 连接密码 (如果 MQ 服务器有安全设置)
```

#### 应用程序特定 MQ 配置 (本专案 `MqConfig.java` 使用)
```yaml
mq-config:
  queueName: YOUR_TARGET_QUEUE_NAME # 要发送/接收消息的队列名称
  messageTtlSeconds: 10           # 默认消息过期时间 (秒)
  reconnectIntervalSeconds: 30    # MQ 连接重试间隔 (秒)
  maxReconnectAttempts: 30        # 在暂停重连之前的最大尝试次数
  reconnectPauseMinutes: 30       # 最大尝试次数后的暂停持续时间 (分钟)
```
**新增配置说明:**
- `reconnectIntervalSeconds`: 应用程序尝试重新连接到 MQ 服务器的间隔时间（秒）。
- `maxReconnectAttempts`: 在连接失败达到此次数后，应用程序将暂停重连。
- `reconnectPauseMinutes`: 当达到最大重连尝试次数后，应用程序将暂停指定的分钟数，然后重新开始尝试连接。

### 2. 启动应用程序

使用 Maven 启动应用程序：

```bash
./mvnw spring-boot:run
```

或使用 Java 直接执行：

```bash
./mvnw clean package
java -jar target/jms-test-0.0.1-SNAPSHOT.jar
```

### 3. MQ 断线重连机制

本应用程序实现了完整的 MQ 断线重连机制：

#### 连接状态监控
- MqConnectionService 维护连接状态，使用 AtomicBoolean 确保线程安全
- 定期检查连接状态并尝试重新连接（每隔 `reconnectIntervalSeconds` 秒）
- 如果连续失败达到 `maxReconnectAttempts` 次，重连机制将暂停 `reconnectPauseMinutes` 分钟

#### 事件发布机制
- 连接暂停时发布 ConnectionPausedEvent 事件
- 连接恢复时发布 ConnectionResumedEvent 事件
- JmsLifecycleManagerService 监听这些事件并管理 JMS 监听器生命周期

#### JMS 监听器生命周期管理
- JmsListenerContainerFactory 设置为不自动启动
- 连接中断时停止 JMS 监听器，避免无效的消息处理
- 连接恢复时启动 JMS 监听器，恢复消息处理

#### 消息发送与接收处理
- MessageSender 在发送消息前检查连接状态，如果连接中断则抛出 MqNotConnectedException
- MessageReceiver 在处理消息前检查连接状态，如果连接中断则抛出 JMSException
- 控制器层捕获这些异常并返回适当的 HTTP 状态码和错误信息

### 4. API 端点

#### 发送消息

##### 发送对象消息

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"content":"这是一条测试消息"}'
```

##### 发送文本消息

```bash
curl -X POST http://localhost:8080/api/messages/send-text \
  -H "Content-Type: application/json" \
  -d '{"text":"这是一条纯文本测试消息"}'
```

##### 发送二进制数据

```bash
curl -X POST http://localhost:8080/api/messages/send-bytes \
  -H "Content-Type: application/json" \
  -d '{"data":"SGVsbG8gV29ybGQh"}'
```

> 注意：二进制数据需要先进行 Base64 编码。上面的例子中，"SGVsbG8gV29ybGQh" 是 "Hello World!" 的 Base64 编码。

**消息发送行为变更**:
如果 MQ 连接不可用（例如，在重连暂停期间或 MQ 服务器确实无法访问），上述发送消息的 API 端点将返回 **HTTP 503 (Service Unavailable)** 错误，并附带一个 JSON 响应体，说明问题。例如：
```json
{
    "success": false,
    "message": "MQ 服务目前不可用。请稍后再试。",
    "errorDetail": "无法发送消息。MQ 未连接。"
}
```

#### MQ 连接管理 API

##### 手动触发重连
此端点允许手动触发一个新的 MQ 重连尝试周期。如果当前处于暂停期，并且暂停时间未结束，则不会立即触发，但会重置尝试计数器（除非手动触发时暂停期刚好结束）。
```bash
curl -X POST http://localhost:8080/api/mq/reconnect/trigger
```
响应示例:
```json
{
    "message": "已触发手动重新连接程序。"
}
```

##### 获取 MQ 连接状态
此端点检索当前 MQ 连接的状态。
```bash
curl -X GET http://localhost:8080/api/mq/status
```
响应示例 (已连接):
```json
{
    "connected": true,
    "currentAttempts": 0,
    "pausedUntil": null
}
```
响应示例 (未连接且正在重试):
```json
{
    "connected": false,
    "currentAttempts": 5,
    "pausedUntil": null
}
```
响应示例 (未连接且处于暂停期):
```json
{
    "connected": false,
    "currentAttempts": 30, // 或上次达到 maxReconnectAttempts 的值
    "pausedUntil": "2023-10-27T15:30:00.123456" // 暂停结束的时间
}
```

## 文件结构

```
src/main/java/com/vance/jms/
├── config/
│   ├── JmsConfig.java           # JMS 配置文件
│   └── MqConfig.java            # MQ 配置文件
├── controller/
│   ├── ConnectionController.java # MQ 连接管理 API 控制器
│   └── MessageController.java   # 消息发送 API 控制器
├── event/
│   ├── ConnectionPausedEvent.java # MQ 连接暂停事件
│   └── ConnectionResumedEvent.java # MQ 连接恢复事件
├── exception/
│   └── MqNotConnectedException.java # MQ 未连接异常
├── model/
│   └── CustomMessage.java       # 自定义消息模型
├── service/
│   ├── JmsLifecycleManagerService.java # JMS 生命周期管理服务
│   ├── MessageReceiver.java     # 消息接收服务
│   ├── MessageSender.java       # 消息发送服务
│   └── MqConnectionService.java # MQ 连接管理服务
└── JmsTestApplication.java      # 应用程序入口
```

## 重要类说明

- **JmsTestApplication**：应用程序入口点，启用 JMS 和调度功能
- **MessageSender**：负责发送消息，并在发送前检查 MQ 连接状态
- **MessageReceiver**：负责接收和处理消息，并在处理前检查 MQ 连接状态
- **MqConnectionService**：管理 MQ 连接状态，提供重新连接机制
- **JmsLifecycleManagerService**：根据 MQ 连接状态管理 JMS 监听器的生命周期

## 待办事项与未来测试计划

以下是未来可以进行的测试和功能扩展：

### 基础功能测试

- [x] 测试不同类型消息的发送和接收 (JSON、文本、二进制等)
- [x] **测试 MQ 连接中断时的错误处理 (部分通过自动重连机制实现)**
- [ ] 测试其他格式消息的发送和接收 (XML等)
- [ ] 测试大量消息的批处理能力
- [ ] 测试消息的持久化和非持久化模式
- [ ] 验证消息过期机制在不同场景下的表现

### 性能测试

- [ ] 进行高并发消息发送测试 (每秒处理消息数)
- [ ] 测量消息处理的延迟时间
- [ ] 评估系统在高负载下的稳定性
- [ ] 测试长时间运行下的内存使用情况

### 错误处理与恢复

- [x] **实现和测试消息重试机制 (应用程序级别的 MQ 连接重试)**
- [ ] 测试消息处理失败时的死信队列功能
- [ ] 开发和测试消息处理的事务支持

### 安全性测试

- [ ] 实现和测试 MQ 连接的 SSL/TLS 加密
- [ ] 测试消息内容加密功能
- [ ] 实现基于角色的消息发送权限控制
- [ ] 审计日志记录与安全事件监控

### 功能扩展

- [ ] 实现消息优先级设置功能
- [ ] 添加消息确认回执机制
- [ ] 开发消息路由功能 (基于内容或标头)
- [ ] 实现消息过滤器功能
- [ ] 添加消息压缩功能以提高效率
- [ ] 开发消息转换器以支持不同格式间的转换

### 监控与管理

- [x] **实现 JMS 连接和队列的监控指标 (部分通过 `/api/mq/status` 端点实现)**
- [ ] 整合 Spring Boot Actuator 提供更全面的健康检查端点
- [ ] 开发管理控制台以查看消息状态
- [ ] 添加警报机制以通知异常情况

### 整合测试

- [ ] 与其他系统的整合测试 (如数据库、缓存等)
- [ ] 测试在微服务架构中的消息传递
- [ ] 实现和测试消息驱动的事件处理流程
- [ ] 测试与其他消息中间件的互操作性

### 文档与示例

- [x] **编写详细的 API 文档 (已更新新端点)**
- [ ] 提供各种使用场景的示例代码
- [ ] 建立性能测试报告模板
- [ ] 编写部署指南和最佳实践

## 贡献指南

欢迎提交 Pull Request 或开 Issue 来改进这个项目。在提交代码前，请确保：

1. 代码符合项目的编码风格
2. 添加了适当的单元测试
3. 所有测试都能通过
4. 更新了相关文档

## 授权

本项目采用 [MIT 授权](LICENSE)。
