# JMS Test Application

This is a Spring Boot-based JMS (Java Message Service) test application that uses IBM MQ as the message broker. It implements message sending and receiving functionality, and features a reconnection mechanism in case of MQ connection loss.

## Features

- Supports sending and receiving various message types:
  - Custom Object Message (CustomMessage)
  - Text Message (TextMessage)
  - Bytes Message (BytesMessage)
- Complete MQ connection management:
  - Automatic reconnection mechanism
  - Connection status monitoring
  - Manual reconnection trigger
- RESTful API interface:
  - Message sending API
  - Connection status query and management API
- Disconnection Reconnection Mechanism:
  - MessageSender checks connection status before sending messages
  - MessageReceiver checks connection status when receiving messages
  - JmsLifecycleManagerService manages the JMS listener lifecycle

## Technical Architecture

- **Spring Boot**: Application framework
- **IBM MQ JMS Spring Boot Starter**: Spring Boot integration for IBM MQ
- **Spring Web**: RESTful API support
- **Spring Actuator**: Application monitoring
- **Lombok**: Simplifies Java code
- **Springdoc OpenAPI**: API documentation generation

## System Requirements

- Java 21 or higher
- Maven 3.8+ (or use the included Maven Wrapper)
- IBM MQ server (local or remote)

## Quick Start

### 1. Configure IBM MQ Connection

Configure your IBM MQ connection parameters in `src/main/resources/application.yml`:

#### IBM MQ Connection Parameters (provided by `ibm-mq-spring-boot-starter`)
```yaml
ibm:
  mq:
    queue-manager: MQJ006D      # MQ Queue Manager name
    channel: DEV.ADMIN.SVRCONN  # MQ Channel name
    conn-name: localhost(3434)  # MQ Connection address and port, e.g., host(port)
    user: mqm                   # MQ Connection username (if the MQ server has security settings)
    # password: yourpassword    # MQ Connection password (if the MQ server has security settings)
```

#### Application-Specific MQ Configuration (used by `MqConfig.java` in this project)
```yaml
mq-config:
  queueName: YOUR_TARGET_QUEUE_NAME # Name of the queue to send/receive messages
  messageTtlSeconds: 10           # Default message time-to-live (seconds)
  reconnectIntervalSeconds: 30    # MQ connection retry interval (seconds)
  maxReconnectAttempts: 30        # Maximum number of attempts before pausing reconnection
  reconnectPauseMinutes: 30       # Pause duration after reaching max attempts (minutes)
```
**Additional Configuration Explanation:**
- `reconnectIntervalSeconds`: The interval (in seconds) at which the application attempts to reconnect to the MQ server.
- `maxReconnectAttempts`: The number of failed connection attempts after which the application will pause reconnection.
- `reconnectPauseMinutes`: The duration (in minutes) for which the application will pause after reaching the maximum reconnection attempts before retrying.

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

### 3. MQ Disconnection Reconnection Mechanism

This application implements a complete MQ disconnection reconnection mechanism:

#### Connection Status Monitoring
- MqConnectionService maintains the connection status using AtomicBoolean for thread safety.
- Periodically checks the connection status (every 10 seconds) and attempts to reconnect if a disconnection is detected.
- Periodically attempts to reconnect (every `reconnectIntervalSeconds` seconds).
- If consecutive failures reach `maxReconnectAttempts`, the reconnection mechanism pauses for `reconnectPauseMinutes` minutes.

#### Event Publishing Mechanism
- Publishes a ConnectionPausedEvent when the connection is paused.
- Publishes a ConnectionResumedEvent when the connection is restored.
- JmsLifecycleManagerService listens for these events and manages the JMS listener lifecycle.

#### JMS Listener Lifecycle Management
- JmsListenerContainerFactory is configured to auto-start, but its startup and shutdown are controlled by JmsLifecycleManagerService.
- Disables the default retry mechanism of DefaultMessageListenerContainer and uses a custom MQ reconnection mechanism.
- Stops the JMS listener when the connection is interrupted to avoid invalid message processing.
- Starts the JMS listener when the connection is restored to resume message processing.
- Manually starts the JMS listener in the main method when the application starts to ensure the listener is properly started.

#### Message Sending and Receiving Handling
- MessageSender checks the connection status before sending a message and throws a MqNotConnectedException if the connection is interrupted.
- MessageReceiver checks the connection status before processing a message and throws a JMSException if the connection is interrupted.
- The controller layer catches these exceptions and returns appropriate HTTP status codes and error messages.

### 4. API Endpoints

#### Sending Messages

##### Sending an Object Message

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{"content":"This is a test message"}'
```

##### Sending a Text Message

```bash
curl -X POST http://localhost:8080/api/messages/send-text \
  -H "Content-Type: application/json" \
  -d '{"text":"This is a plain text test message"}'
```

##### Sending Binary Data

```bash
curl -X POST http://localhost:8080/api/messages/send-bytes \
  -H "Content-Type: application/json" \
  -d '{"data":"SGVsbG8gV29ybGQh"}'
```

> Note: Binary data needs to be Base64 encoded first. In the example above, "SGVsbG8gV29ybGQh" is the Base64 encoding of "Hello World!".

**Message Sending Behavior Change**:
If the MQ connection is unavailable (e.g., during a reconnection pause or if the MQ server is genuinely unreachable), the above message sending API endpoints will return an **HTTP 503 (Service Unavailable)** error with a JSON response body explaining the issue. For example:
```json
{
    "success": false,
    "message": "MQ service is currently unavailable. Please try again later.",
    "errorDetail": "Unable to send message. MQ is not connected."
}
```

#### MQ Connection Management API

##### Manually Trigger Reconnection
This endpoint allows you to manually trigger a new MQ reconnection attempt cycle. If currently in a paused state and the pause duration hasn't elapsed, it won't trigger immediately, but it will reset the attempt counter (unless the pause period is just ending when manually triggered).
```bash
curl -X POST http://localhost:8080/api/mq/reconnect/trigger
```
Response Example:
```json
{
    "message": "Manual reconnection process triggered."
}
```

##### Get MQ Connection Status
This endpoint retrieves the current status of the MQ connection.
```bash
curl -X GET http://localhost:8080/api/mq/status
```
Response Example (Connected):
```json
{
    "connected": true,
    "currentAttempts": 0,
    "pausedUntil": null
}
```
Response Example (Not Connected and Retrying):
```json
{
    "connected": false,
    "currentAttempts": 5,
    "pausedUntil": null
}
```
Response Example (Not Connected and Paused):
```json
{
    "connected": false,
    "currentAttempts": 30, // Or the last value when maxReconnectAttempts was reached
    "pausedUntil": "2023-10-27T15:30:00.123456" // The time the pause ends
}
```

## File Structure

```
src/main/java/com/vance/jms/
├── config/
│   ├── JmsConfig.java           # JMS Configuration class
│   └── MqConfig.java            # MQ Configuration class
├── controller/
│   ├── ConnectionController.java # MQ Connection Management API Controller
│   └── MessageController.java   # Message Sending API Controller
├── event/
│   ├── ConnectionPausedEvent.java # MQ Connection Paused Event
│   └── ConnectionResumedEvent.java # MQ Connection Resumed Event
├── exception/
│   └── MqNotConnectedException.java # MQ Not Connected Exception
├── model/
│   └── CustomMessage.java       # Custom Message Model
├── service/
│   ├── JmsLifecycleManagerService.java # JMS Lifecycle Management Service
│   ├── MessageReceiver.java     # Message Receiving Service
│   ├── MessageSender.java       # Message Sending Service
│   └── MqConnectionService.java # MQ Connection Management Service
└── JmsTestApplication.java      # Application Entry Point
```

## Important Class Descriptions

- **JmsTestApplication**: Application entry point, enables JMS and scheduling features
- **MessageSender**: Responsible for sending messages and checking the MQ connection status before sending
- **MessageReceiver**: Responsible for receiving and processing messages and checking the MQ connection status before processing
- **MqConnectionService**: Manages the MQ connection status and provides a reconnection mechanism
- **JmsLifecycleManagerService**: Manages the lifecycle of the JMS listener based on the MQ connection status

## To-Do Items and Future Testing Plans

The following are tests and feature extensions that can be performed in the future:

### Basic Functionality Testing

- [x] Test sending and receiving different types of messages (JSON, text, binary, etc.)
- [x] **Test error handling when the MQ connection is interrupted (partially implemented through the automatic reconnection mechanism)**
- [ ] Test sending and receiving messages in other formats (XML, etc.)
- [ ] Test the batch processing capability of large numbers of messages
- [ ] Test message persistence and non-persistence modes
- [ ] Verify the behavior of the message expiration mechanism in different scenarios

### Performance Testing

- [ ] Conduct high-concurrency message sending tests (number of messages processed per second)
- [ ] Measure message processing latency
- [ ] Evaluate system stability under high load
- [ ] Test memory usage during long-term operation

### Error Handling and Recovery

- [x] **Implement and test message retry mechanism (application-level MQ connection retry)**
- [ ] Test the dead letter queue functionality when message processing fails
- [ ] Develop and test transaction support for message processing

### Security Testing

- [ ] Implement and test SSL/TLS encryption for MQ connections
- [ ] Test message content encryption functionality
- [ ] Implement role-based message sending permission control
- [ ] Audit log recording and security event monitoring

### Feature Expansion

- [ ] Implement message priority setting functionality
- [ ] Add a message confirmation receipt mechanism
- [ ] Develop message routing functionality (based on content or headers)
- [ ] Implement message filter functionality
- [ ] Add message compression functionality to improve efficiency
- [ ] Develop message converters to support conversions between different formats

### Monitoring and Management

- [x] **Implement monitoring metrics for JMS connections and queues (partially implemented through the `/api/mq/status` endpoint)**
- [ ] Integrate Spring Boot Actuator to provide more comprehensive health check endpoints
- [ ] Develop a management console to view message status
- [ ] Add an alert mechanism to notify of abnormal situations

### Integration Testing

- [ ] Integration testing with other systems (such as databases, caches, etc.)
- [ ] Test message passing in a microservices architecture
- [ ] Implement and test message-driven event processing flows
- [ ] Test interoperability with other message middleware

### Documentation and Examples

- [x] **Write detailed API documentation (updated with new endpoints)**
- [ ] Provide example code for various usage scenarios
- [ ] Create a performance test report template
- [ ] Write deployment guides and best practices

## Contribution Guidelines

Feel free to submit Pull Requests or open Issues to improve this project. Before submitting code, please ensure that:

1. The code conforms to the project's coding style
2. Appropriate unit tests have been added
3. All tests pass
4. Relevant documentation has been updated

## License

This project is licensed under the [MIT License](LICENSE).
