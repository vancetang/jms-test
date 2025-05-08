# JMS Test Project

This is a Spring Boot-based JMS (Java Message Service) test project designed for testing message sending and receiving functionality with IBM MQ. The project provides simple REST API endpoints, allowing users to send messages to IBM MQ queues and configure various message properties, such as expiration time.

## Key Features

- Based on Spring Boot 3.4.5 and Java 21
- Integrates IBM MQ JMS Spring Boot Starter
- Supports sending object messages and text messages
- Automatic message expiration (currently set to 10 seconds)
- RESTful API interface
- Extensible message processing architecture

## System Requirements

- Java 21 or later
- Maven 3.8+ (or use the included Maven Wrapper)
- IBM MQ server (local or remote)

## Quick Start

### 1. Configure IBM MQ Connection

Configure your IBM MQ connection parameters in `src/main/resources/application.yml`:

```yaml
ibm:
  mq:
    queue-manager: MQJ006D
    channel: DEV.ADMIN.SVRCONN
    conn-name: localhost(3434)
    user: mqm
```

### 2. Start the Application

Start the application using Maven:

```bash
./mvnw spring-boot:run
```

Or execute directly using Java:

```bash
./mvnw clean package
java -jar target/jms-test-0.0.1-SNAPSHOT.jar
```

### 3. Send Messages

#### Send Object Message

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"content":"這是一條測試訊息"}'
```

#### Send Text Message

```bash
curl -X POST http://localhost:8080/api/messages/send-text \
  -H "Content-Type: application/json" \
  -d '{"text":"這是一條純文本測試訊息"}'
```

## Project Structure

```
src/main/java/com/vance/jms/
├── config/
│   └── JmsConfig.java           # JMS Configuration Class
├── constant/
│   └── Constant.java            # Constant Definitions
├── controller/
│   └── MessageController.java   # REST API Controller
├── model/
│   └── Message.java             # Message Model Class
├── service/
│   ├── MessageSender.java       # Message Sending Service
│   └── MessageReceiver.java     # Message Receiving Service
└── JmsTestApplication.java      # Application Entry Point
```

## To-Do List and Future Test Plans

The following are future tests and feature enhancements that can be performed:

### Basic Functionality Testing

- [ ] Test sending and receiving different types of messages (JSON, XML, binary, etc.)
- [ ] Test batch processing capabilities for large numbers of messages
- [ ] Test persistent and non-persistent message modes
- [ ] Verify message expiration mechanism in different scenarios

### Performance Testing

- [ ] Conduct high-concurrency message sending tests (messages processed per second)
- [ ] Measure message processing latency
- [ ] Evaluate system stability under high load
- [ ] Test memory usage during long-running operations

### Error Handling and Recovery

- [ ] Test error handling when MQ connection is interrupted
- [ ] Implement and test message retry mechanism
- [ ] Test dead letter queue functionality when message processing fails
- [ ] Develop and test transaction support for message processing

### Security Testing

- [ ] Implement and test SSL/TLS encryption for MQ connection
- [ ] Test message content encryption
- [ ] Implement role-based message sending permission control
- [ ] Audit log recording and security event monitoring

### Feature Enhancements

- [ ] Implement message priority setting
- [ ] Add message confirmation receipt mechanism
- [ ] Develop message routing functionality (based on content or headers)
- [ ] Implement message filter functionality
- [ ] Add message compression to improve efficiency
- [ ] Develop message converters to support conversions between different formats

### Monitoring and Management

- [ ] Integrate Spring Boot Actuator to provide health check endpoints
- [ ] Implement monitoring metrics for JMS connections and queues
- [ ] Develop a management console to view message status
- [ ] Add an alerting mechanism to notify of abnormal situations

### Integration Testing

- [ ] Integration testing with other systems (e.g., databases, caches, etc.)
- [ ] Test message passing in a microservices architecture
- [ ] Implement and test message-driven event processing flows
- [ ] Test interoperability with other message middleware

### Documentation and Examples

- [ ] Write detailed API documentation
- [ ] Provide example code for various usage scenarios
- [ ] Create a performance test report template
- [ ] Write a deployment guide and best practices

## Contribution Guidelines

Feel free to submit a Pull Request or open an Issue to improve this project. Before submitting code, please ensure that:

1. The code conforms to the project's coding style
2. Appropriate unit tests have been added
3. All tests pass
4. Relevant documentation has been updated

## License

This project is licensed under the [MIT License](LICENSE).
