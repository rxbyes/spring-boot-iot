# Codex Roadmap

## Phase 1
### Task 1 [DONE]
Create Maven multi-module structure:
- spring-boot-iot-common
- spring-boot-iot-framework
- spring-boot-iot-auth
- spring-boot-iot-system
- spring-boot-iot-device
- spring-boot-iot-protocol
- spring-boot-iot-message
- spring-boot-iot-admin

### Task 2 [DONE]
Add base infrastructure:
- R
- PageResult
- BizException
- BaseEntity
- GlobalExceptionHandler
- SecurityConfig
- IotProperties

### Task 3 [DONE]
Add entities and mappers:
- Product
- ProductModel
- Device
- DeviceProperty
- DeviceMessageLog

### Task 4 [DONE]
Add services and controllers:
- ProductService / Controller
- DeviceService / Controller
- DevicePropertyController
- DeviceMessageLogController

### Task 5 [DONE]
Implement HTTP reporting pipeline:
- DeviceReportRequest
- DeviceHttpController
- UpMessageDispatcher
- ProtocolAdapter
- ProtocolAdapterRegistry
- MqttJsonProtocolAdapter
- DeviceMessageServiceImpl

### Task 6 [DONE]
Verify:
- insert message log
- update latest property
- update online status
- validate bad protocol and missing device errors
- add unit tests for DeviceMessageServiceImpl

## Phase 2
### Status: TODO
- MQTT consumer
- Device shadow
- Gateway topology
- Command record
- Telemetry query

## Phase 3
### Status: TODO
- Rule engine
- Alarm center
- OTA
- Open API
- Monitoring
