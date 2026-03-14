# Codex Roadmap

## Phase 1
### Task 1
Create Maven multi-module structure:
- spring-boot-iot-common
- spring-boot-iot-framework
- spring-boot-iot-auth
- spring-boot-iot-system
- spring-boot-iot-device
- spring-boot-iot-protocol
- spring-boot-iot-message
- spring-boot-iot-admin

### Task 2
Add base infrastructure:
- R
- PageResult
- BizException
- BaseEntity
- GlobalExceptionHandler
- SecurityConfig
- IotProperties

### Task 3
Add entities and mappers:
- Product
- ProductModel
- Device
- DeviceProperty
- DeviceMessageLog

### Task 4
Add services and controllers:
- ProductService / Controller
- DeviceService / Controller
- DevicePropertyController
- DeviceMessageLogController

### Task 5
Implement HTTP reporting pipeline:
- DeviceReportRequest
- DeviceHttpController
- UpMessageDispatcher
- ProtocolAdapter
- ProtocolAdapterRegistry
- MqttJsonProtocolAdapter
- DeviceMessageServiceImpl

### Task 6
Verify:
- insert message log
- update latest property
- update online status

## Phase 2
- MQTT consumer
- Device shadow
- Gateway topology
- Command record
- Telemetry query

## Phase 3
- Rule engine
- Alarm center
- OTA
- Open API
- Monitoring