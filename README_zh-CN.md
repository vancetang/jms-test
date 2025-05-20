# JMS 测试项目

这是一个基于 Spring Boot 的 JMS (Java Message Service) 测试项目，用于测试与 IBM MQ 的消息发送和接收功能。项目提供了简单的 REST API 端点，允许用户发送消息到 IBM MQ 队列，并可以配置消息的各种属性，如过期时间等。

## 功能特点

- 基于 Spring Boot 3.4.5 和 Java 21
- 整合 IBM MQ JMS Spring Boot Starter
- 支持发送对象消息、文本消息和二进制数据
- 消息自动过期功能 (目前设置为 10 秒)
- RESTful API 接口
- 可扩展的消息处理架构

## 系统需求

- Java 21 或更高版本
- Maven 3.8+ (或使用包含的 Maven Wrapper)
- IBM MQ 服务器 (本地或远程)

## 快速开始

### 1. 配置 IBM MQ 连接

在 `src/main/resources/application.yml` 中配置您的 IBM MQ 连接参数：

```yaml
ibm:
  mq:
    queue-manager: MQJ006D
    channel: DEV.ADMIN.SVRCONN
    conn-name: localhost(3434)
    user: mqm
```

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

### 3. 发送消息

#### 发送对象消息

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"content":"这是一条测试消息"}'
```

#### 发送文本消息

```bash
curl -X POST http://localhost:8080/api/messages/send-text \
  -H "Content-Type: application/json" \
  -d '{"text":"这是一条纯文本测试消息"}'
```

#### 发送二进制数据

```bash
curl -X POST http://localhost:8080/api/messages/send-bytes \
  -H "Content-Type: application/json" \
  -d '{"data":"SGVsbG8gV29ybGQh"}'
```

> 注意：二进制数据需要先进行 Base64 编码。上面的例子中，"SGVsbG8gV29ybGQh" 是 "Hello World!" 的 Base64 编码。

## 项目结构

```
src/main/java/com/vance/jms/
├── config/
│   └── JmsConfig.java           # JMS 配置类
├── constant/
│   └── Constant.java            # 常量定义
├── controller/
│   └── MessageController.java   # REST API 控制器
├── model/
│   └── Message.java             # 消息模型类
├── service/
│   ├── MessageSender.java       # 消息发送服务
│   └── MessageReceiver.java     # 消息接收服务
└── JmsTestApplication.java      # 应用程序入口
```

## 待办事项与未来测试计划

以下是未来可以进行的测试和功能扩展：

### 基础功能测试

- [x] 测试不同类型消息的发送和接收 (JSON、文本、二进制等)
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

- [ ] 测试 MQ 连接中断时的错误处理
- [ ] 实现和测试消息重试机制
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

- [ ] 整合 Spring Boot Actuator 提供健康检查端点
- [ ] 实现 JMS 连接和队列的监控指标
- [ ] 开发管理控制台以查看消息状态
- [ ] 添加警报机制以通知异常情况

### 整合测试

- [ ] 与其他系统的整合测试 (如数据库、缓存等)
- [ ] 测试在微服务架构中的消息传递
- [ ] 实现和测试消息驱动的事件处理流程
- [ ] 测试与其他消息中间件的互操作性

### 文档与示例

- [ ] 编写详细的 API 文档
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
