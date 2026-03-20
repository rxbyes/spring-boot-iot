# 架构图文本版

```text
Device / Gateway
      |
      v
Access Layer (HTTP / MQTT / TCP)
      |
      v
Protocol Layer (ProtocolAdapter / Registry)
      |
      v
Business Layer (DeviceMessageService / Product / Device)
      |
      +--> iot_message_log
      +--> iot_device_property
      +--> iot_device
      |
      v
Future Extensions
- Rule Engine
- Alarm Center
- OTA
- Open API
```
