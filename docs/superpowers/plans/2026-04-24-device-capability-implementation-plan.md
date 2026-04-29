# Device Capability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add product-type-driven device capability presentation and asynchronous command execution for monitoring, warning, and video devices in the device asset center.

**Architecture:** Product `metadataJson.governance` remains the source of truth for capability type and subtype. The device module resolves visible capabilities and exposes read APIs; the message module validates and publishes capability commands; feedback updates existing `iot_command_record` state by `msgid`. The UI consumes backend capabilities instead of hardcoding product type rules.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Maven, Vue 3, TypeScript, Element Plus, Vitest.

---

## File Structure

### Backend: Device Module

- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityType.java`
  - Enum for `COLLECTING / MONITORING / WARNING / VIDEO / UNKNOWN`.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/WarningDeviceKind.java`
  - Enum for `BROADCAST / LED / FLASH / UNKNOWN`.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/VideoDeviceKind.java`
  - Enum for `FIXED_CAMERA / PTZ_CAMERA / UNKNOWN`.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityCode.java`
  - Stable capability code constants.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/ProductCapabilityMetadata.java`
  - Parsed product capability metadata value object.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/ProductCapabilityMetadataParser.java`
  - Parse product `metadataJson.governance`.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityDefinition.java`
  - Internal capability definition with code, name, group, params schema.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityRegistry.java`
  - Built-in capability matrix.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceCapabilityExecuteDTO.java`
  - Execute request payload.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityVO.java`
  - Single capability response.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityOverviewVO.java`
  - Capability overview response.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityExecuteResultVO.java`
  - Execute response.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CommandRecordPageItemVO.java`
  - Command list item.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceCapabilityService.java`
  - Device capability read and execute orchestration API.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceCapabilityCommandGateway.java`
  - Device module gateway interface implemented by message module.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/DeviceCapabilityCommandRequest.java`
  - Gateway command request.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/DeviceCapabilityCommandResult.java`
  - Gateway command result.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java`
  - Validate device/product/data scope, resolve capabilities, call command gateway.
- Create `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCapabilityController.java`
  - `GET /api/device/{deviceCode}/capabilities`, `POST /api/device/{deviceCode}/capabilities/{capabilityCode}/execute`, `GET /api/device/{deviceCode}/commands`.
- Modify `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CommandRecordService.java`
  - Add page query support if not already present.
- Modify `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CommandRecordServiceImpl.java`
  - Implement command list by device and tenant/data scope.
- Modify `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/CommandRecordMapper.java`
  - Add query method only if MyBatis-Plus wrapper is insufficient.

### Backend: Message Module

- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/DeviceCapabilityDownCommandService.java`
  - Message module service for capability command publish.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityCommandGatewayImpl.java`
  - Implements device module gateway.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityDownCommandServiceImpl.java`
  - Validates capability params, builds topic and raw payload, creates command record, publishes raw MQTT.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityCommandPayload.java`
  - Topic, payload, command type, service identifier, command id.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/WarningCapabilityPayloadBuilder.java`
  - Broadcast, LED, flash payload builder.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/VideoCapabilityPayloadBuilder.java`
  - Video semantic payload builder; use JSON payload until real vendor protocol is supplied.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityFeedbackParser.java`
  - Query-string feedback parser for `$cmd / result / msgid / message`.
- Create `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityFeedbackHandler.java`
  - Updates command records from parsed feedback.
- Modify MQTT/up-message path after topic routing, likely in `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java` or a stage handler if feedback interception already has a better local hook.
  - Intercept `/broadcast/{deviceCode}/feedback`, `/iot/broadcast/{deviceCode}/feedback`, `/iot/led/{deviceCode}/feedback`, `/iot/flash/{deviceCode}/feedback` and route to feedback handler without writing latest properties.

### Frontend

- Modify `spring-boot-iot-ui/src/types/api.ts`
  - Add capability and command types.
- Modify `spring-boot-iot-ui/src/api/device.ts`
  - Add `getDeviceCapabilities`, `executeDeviceCapability`, `pageDeviceCommands`.
- Create `spring-boot-iot-ui/src/components/device/DeviceCapabilityPanel.vue`
  - Shows capability type, grouped buttons, disabled reasons, command list.
- Create `spring-boot-iot-ui/src/components/device/DeviceCapabilityExecuteDrawer.vue`
  - Parameter form for selected capability.
- Modify `spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue`
  - Add registered-device capability section.
- Modify `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
  - Add row menu `设备操作`, fetch capability overview when detail opens, pass it to detail workbench.
- Add tests in `spring-boot-iot-ui/src/__tests__/components/device/DeviceCapabilityPanel.test.ts`.
- Add or update `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`.

### Docs

- Modify `docs/02-业务功能与流程说明.md`
  - Add device capability behavior and command feedback lifecycle.
- Modify `docs/03-接口规范与接口清单.md`
  - Document new capability and command APIs.
- Modify `docs/05-自动化测试与质量保障.md`
  - Add capability command test scope.
- Check `README.md` and `AGENTS.md`.
  - Update only if the capability becomes part of baseline project status.

---

### Task 1: Product Capability Metadata Parser

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityType.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/WarningDeviceKind.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/VideoDeviceKind.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/ProductCapabilityMetadata.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/ProductCapabilityMetadataParser.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/capability/ProductCapabilityMetadataParserTest.java`

- [ ] **Step 1: Write parser tests**

Create `ProductCapabilityMetadataParserTest.java`:

```java
package com.ghlzm.iot.device.capability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCapabilityMetadataParserTest {

    private final ProductCapabilityMetadataParser parser = new ProductCapabilityMetadataParser();

    @Test
    void parsesWarningBroadcastMetadata() {
        ProductCapabilityMetadata metadata = parser.parse("""
                {"governance":{"productCapabilityType":"WARNING","warningDeviceKind":"BROADCAST"}}
                """);

        assertThat(metadata.capabilityType()).isEqualTo(DeviceCapabilityType.WARNING);
        assertThat(metadata.warningDeviceKind()).isEqualTo(WarningDeviceKind.BROADCAST);
        assertThat(metadata.videoDeviceKind()).isEqualTo(VideoDeviceKind.UNKNOWN);
    }

    @Test
    void parsesVideoPtzMetadata() {
        ProductCapabilityMetadata metadata = parser.parse("""
                {"governance":{"productCapabilityType":"VIDEO","videoDeviceKind":"PTZ_CAMERA"}}
                """);

        assertThat(metadata.capabilityType()).isEqualTo(DeviceCapabilityType.VIDEO);
        assertThat(metadata.warningDeviceKind()).isEqualTo(WarningDeviceKind.UNKNOWN);
        assertThat(metadata.videoDeviceKind()).isEqualTo(VideoDeviceKind.PTZ_CAMERA);
    }

    @Test
    void returnsUnknownForBlankOrInvalidMetadata() {
        assertThat(parser.parse(null).capabilityType()).isEqualTo(DeviceCapabilityType.UNKNOWN);
        assertThat(parser.parse("").capabilityType()).isEqualTo(DeviceCapabilityType.UNKNOWN);
        assertThat(parser.parse("{bad json").capabilityType()).isEqualTo(DeviceCapabilityType.UNKNOWN);
    }

    @Test
    void isCaseInsensitiveForKnownValues() {
        ProductCapabilityMetadata metadata = parser.parse("""
                {"governance":{"productCapabilityType":"warning","warningDeviceKind":"flash"}}
                """);

        assertThat(metadata.capabilityType()).isEqualTo(DeviceCapabilityType.WARNING);
        assertThat(metadata.warningDeviceKind()).isEqualTo(WarningDeviceKind.FLASH);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=ProductCapabilityMetadataParserTest test
```

If `.mvn/settings.xml` is missing, run:

```bash
mvn -pl spring-boot-iot-device -Dtest=ProductCapabilityMetadataParserTest test
```

Expected: compilation fails because parser classes do not exist.

- [ ] **Step 3: Implement metadata enums and parser**

Create `DeviceCapabilityType.java`:

```java
package com.ghlzm.iot.device.capability;

public enum DeviceCapabilityType {
    COLLECTING,
    MONITORING,
    WARNING,
    VIDEO,
    UNKNOWN;

    public static DeviceCapabilityType from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (DeviceCapabilityType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
```

Create `WarningDeviceKind.java`:

```java
package com.ghlzm.iot.device.capability;

public enum WarningDeviceKind {
    BROADCAST,
    LED,
    FLASH,
    UNKNOWN;

    public static WarningDeviceKind from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (WarningDeviceKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return UNKNOWN;
    }
}
```

Create `VideoDeviceKind.java`:

```java
package com.ghlzm.iot.device.capability;

public enum VideoDeviceKind {
    FIXED_CAMERA,
    PTZ_CAMERA,
    UNKNOWN;

    public static VideoDeviceKind from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (VideoDeviceKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return UNKNOWN;
    }
}
```

Create `ProductCapabilityMetadata.java`:

```java
package com.ghlzm.iot.device.capability;

public record ProductCapabilityMetadata(
        DeviceCapabilityType capabilityType,
        WarningDeviceKind warningDeviceKind,
        VideoDeviceKind videoDeviceKind
) {
    public static ProductCapabilityMetadata unknown() {
        return new ProductCapabilityMetadata(
                DeviceCapabilityType.UNKNOWN,
                WarningDeviceKind.UNKNOWN,
                VideoDeviceKind.UNKNOWN
        );
    }
}
```

Create `ProductCapabilityMetadataParser.java`:

```java
package com.ghlzm.iot.device.capability;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProductCapabilityMetadataParser {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public ProductCapabilityMetadata parse(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return ProductCapabilityMetadata.unknown();
        }
        try {
            JsonNode root = objectMapper.readTree(metadataJson);
            JsonNode governance = root == null ? null : root.get("governance");
            if (governance == null || governance.isNull()) {
                return ProductCapabilityMetadata.unknown();
            }
            return new ProductCapabilityMetadata(
                    DeviceCapabilityType.from(text(governance, "productCapabilityType")),
                    WarningDeviceKind.from(text(governance, "warningDeviceKind")),
                    VideoDeviceKind.from(text(governance, "videoDeviceKind"))
            );
        } catch (Exception ignored) {
            return ProductCapabilityMetadata.unknown();
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
```

- [ ] **Step 4: Run parser tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=ProductCapabilityMetadataParserTest test
```

Expected: tests pass.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/capability/ProductCapabilityMetadataParserTest.java
git commit -m "feat: parse product device capability metadata"
```

---

### Task 2: Built-In Capability Registry

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityCode.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityDefinition.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability/DeviceCapabilityRegistry.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/capability/DeviceCapabilityRegistryTest.java`

- [ ] **Step 1: Write registry tests**

Create `DeviceCapabilityRegistryTest.java`:

```java
package com.ghlzm.iot.device.capability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceCapabilityRegistryTest {

    private final DeviceCapabilityRegistry registry = new DeviceCapabilityRegistry();

    @Test
    void monitoringDevicesExposeMaintenanceCapabilities() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.MONITORING, WarningDeviceKind.UNKNOWN, VideoDeviceKind.UNKNOWN)
        );

        assertThat(codes(capabilities)).containsExactly("power_switch", "reboot", "firmware_upgrade");
    }

    @Test
    void warningBroadcastDevicesExposeBroadcastCapabilities() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.WARNING, WarningDeviceKind.BROADCAST, VideoDeviceKind.UNKNOWN)
        );

        assertThat(codes(capabilities)).containsExactly(
                "broadcast_play", "broadcast_stop", "broadcast_volume", "reboot"
        );
        assertThat(capabilities.stream().filter(item -> item.code().equals("broadcast_play")).findFirst())
                .get()
                .extracting(item -> item.paramsSchema().containsKey("content"))
                .isEqualTo(true);
    }

    @Test
    void warningLedDevicesExposeLedCapabilities() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.WARNING, WarningDeviceKind.LED, VideoDeviceKind.UNKNOWN)
        );

        assertThat(codes(capabilities)).containsExactly("led_program", "led_stop", "reboot");
    }

    @Test
    void warningFlashDevicesExposeFlashCapabilities() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.WARNING, WarningDeviceKind.FLASH, VideoDeviceKind.UNKNOWN)
        );

        assertThat(codes(capabilities)).containsExactly("flash_control", "flash_stop", "reboot");
    }

    @Test
    void ptzVideoDevicesExposeVideoAndAzimuthCapabilities() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.VIDEO, WarningDeviceKind.UNKNOWN, VideoDeviceKind.PTZ_CAMERA)
        );

        assertThat(codes(capabilities)).containsExactly("video_play", "video_stop", "video_turn_azimuth");
    }

    @Test
    void collectingDevicesDoNotExposeControlCapabilitiesByDefault() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.COLLECTING, WarningDeviceKind.UNKNOWN, VideoDeviceKind.UNKNOWN)
        );

        assertThat(capabilities).isEmpty();
    }

    private List<String> codes(List<DeviceCapabilityDefinition> capabilities) {
        return capabilities.stream().map(DeviceCapabilityDefinition::code).toList();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=DeviceCapabilityRegistryTest test
```

Expected: compilation fails because registry classes do not exist.

- [ ] **Step 3: Implement registry**

Create `DeviceCapabilityCode.java`:

```java
package com.ghlzm.iot.device.capability;

public final class DeviceCapabilityCode {

    public static final String POWER_SWITCH = "power_switch";
    public static final String REBOOT = "reboot";
    public static final String FIRMWARE_UPGRADE = "firmware_upgrade";
    public static final String BROADCAST_PLAY = "broadcast_play";
    public static final String BROADCAST_STOP = "broadcast_stop";
    public static final String BROADCAST_VOLUME = "broadcast_volume";
    public static final String LED_PROGRAM = "led_program";
    public static final String LED_STOP = "led_stop";
    public static final String FLASH_CONTROL = "flash_control";
    public static final String FLASH_STOP = "flash_stop";
    public static final String VIDEO_PLAY = "video_play";
    public static final String VIDEO_STOP = "video_stop";
    public static final String VIDEO_TURN_AZIMUTH = "video_turn_azimuth";

    private DeviceCapabilityCode() {
    }
}
```

Create `DeviceCapabilityDefinition.java`:

```java
package com.ghlzm.iot.device.capability;

import java.util.Map;

public record DeviceCapabilityDefinition(
        String code,
        String name,
        String group,
        boolean requiresOnline,
        Map<String, Map<String, Object>> paramsSchema
) {
}
```

Create `DeviceCapabilityRegistry.java`:

```java
package com.ghlzm.iot.device.capability;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DeviceCapabilityRegistry {

    public List<DeviceCapabilityDefinition> resolve(ProductCapabilityMetadata metadata) {
        ProductCapabilityMetadata actual = metadata == null ? ProductCapabilityMetadata.unknown() : metadata;
        return switch (actual.capabilityType()) {
            case MONITORING -> List.of(powerSwitch(), reboot(), firmwareUpgrade());
            case WARNING -> resolveWarning(actual.warningDeviceKind());
            case VIDEO -> resolveVideo(actual.videoDeviceKind());
            case COLLECTING, UNKNOWN -> List.of();
        };
    }

    public DeviceCapabilityDefinition require(String capabilityCode, ProductCapabilityMetadata metadata) {
        return resolve(metadata).stream()
                .filter(item -> item.code().equals(capabilityCode))
                .findFirst()
                .orElse(null);
    }

    private List<DeviceCapabilityDefinition> resolveWarning(WarningDeviceKind kind) {
        return switch (kind) {
            case BROADCAST -> List.of(broadcastPlay(), broadcastStop(), broadcastVolume(), reboot());
            case LED -> List.of(ledProgram(), ledStop(), reboot());
            case FLASH -> List.of(flashControl(), flashStop(), reboot());
            case UNKNOWN -> List.of();
        };
    }

    private List<DeviceCapabilityDefinition> resolveVideo(VideoDeviceKind kind) {
        if (kind == VideoDeviceKind.PTZ_CAMERA) {
            return List.of(videoPlay(), videoStop(), videoTurnAzimuth());
        }
        if (kind == VideoDeviceKind.FIXED_CAMERA) {
            return List.of(videoPlay(), videoStop());
        }
        return List.of();
    }

    private DeviceCapabilityDefinition powerSwitch() {
        return capability(DeviceCapabilityCode.POWER_SWITCH, "开关", "基础维护", true, Map.of(
                "enabled", number("开关状态", 0, 1, true)
        ));
    }

    private DeviceCapabilityDefinition reboot() {
        return capability(DeviceCapabilityCode.REBOOT, "重启", "基础维护", true, Map.of());
    }

    private DeviceCapabilityDefinition firmwareUpgrade() {
        return capability(DeviceCapabilityCode.FIRMWARE_UPGRADE, "固件升级", "基础维护", true, Map.of(
                "version", text("目标版本", true)
        ));
    }

    private DeviceCapabilityDefinition broadcastPlay() {
        return capability(DeviceCapabilityCode.BROADCAST_PLAY, "播放内容", "广播预警", true, Map.of(
                "content", text("播报内容", true),
                "bNum", number("播报次数", -1, 999, false),
                "volume", number("音量", 0, 100, false)
        ));
    }

    private DeviceCapabilityDefinition broadcastStop() {
        return capability(DeviceCapabilityCode.BROADCAST_STOP, "停止播放", "广播预警", true, Map.of());
    }

    private DeviceCapabilityDefinition broadcastVolume() {
        return capability(DeviceCapabilityCode.BROADCAST_VOLUME, "音量控制", "广播预警", true, Map.of(
                "volume", number("音量", 0, 100, true)
        ));
    }

    private DeviceCapabilityDefinition ledProgram() {
        return capability(DeviceCapabilityCode.LED_PROGRAM, "节目控制", "情报板", true, Map.of(
                "type", number("节目编号", 1, 10, true),
                "brigh", number("亮度", 1, 8, true),
                "freq", number("频次", 1, 4, true)
        ));
    }

    private DeviceCapabilityDefinition ledStop() {
        return capability(DeviceCapabilityCode.LED_STOP, "关闭内容", "情报板", true, Map.of());
    }

    private DeviceCapabilityDefinition flashControl() {
        return capability(DeviceCapabilityCode.FLASH_CONTROL, "爆闪控制", "爆闪灯", true, Map.of(
                "type", number("灯控类型", 0, 3, true),
                "brigh", number("亮度", 1, 8, true),
                "freq", number("频次", 1, 4, true)
        ));
    }

    private DeviceCapabilityDefinition flashStop() {
        return capability(DeviceCapabilityCode.FLASH_STOP, "关闭设备", "爆闪灯", true, Map.of());
    }

    private DeviceCapabilityDefinition videoPlay() {
        return capability(DeviceCapabilityCode.VIDEO_PLAY, "播放视频", "视频控制", true, Map.of(
                "streamUrl", text("播放地址", false),
                "channel", text("通道", false),
                "durationSeconds", number("播放时长", 1, 86400, false)
        ));
    }

    private DeviceCapabilityDefinition videoStop() {
        return capability(DeviceCapabilityCode.VIDEO_STOP, "停止播放视频", "视频控制", true, Map.of(
                "channel", text("通道", false)
        ));
    }

    private DeviceCapabilityDefinition videoTurnAzimuth() {
        return capability(DeviceCapabilityCode.VIDEO_TURN_AZIMUTH, "按方位角转向", "视频控制", true, Map.of(
                "azimuth", number("方位角", 0, 360, true)
        ));
    }

    private DeviceCapabilityDefinition capability(String code,
                                                  String name,
                                                  String group,
                                                  boolean requiresOnline,
                                                  Map<String, Map<String, Object>> paramsSchema) {
        return new DeviceCapabilityDefinition(code, name, group, requiresOnline, paramsSchema);
    }

    private Map<String, Object> text(String label, boolean required) {
        return Map.of("type", "string", "label", label, "required", required);
    }

    private Map<String, Object> number(String label, int min, int max, boolean required) {
        return Map.of("type", "integer", "label", label, "min", min, "max", max, "required", required);
    }
}
```

- [ ] **Step 4: Run registry tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=DeviceCapabilityRegistryTest test
```

Expected: tests pass.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/capability/DeviceCapabilityRegistryTest.java
git commit -m "feat: add built-in device capability registry"
```

---

### Task 3: Device Capability Read API

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityOverviewVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceCapabilityService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCapabilityController.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImplTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceCapabilityControllerTest.java`

- [ ] **Step 1: Write service test**

Create `DeviceCapabilityServiceImplTest.java` with Mockito-based unit tests:

```java
package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.capability.DeviceCapabilityRegistry;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadataParser;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceCapabilityCommandGateway;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceCapabilityServiceImplTest {

    private DeviceService deviceService;
    private ProductService productService;
    private DeviceCapabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        deviceService = mock(DeviceService.class);
        productService = mock(ProductService.class);
        DeviceCapabilityCommandGateway commandGateway = mock(DeviceCapabilityCommandGateway.class);
        service = new DeviceCapabilityServiceImpl(
                deviceService,
                productService,
                new ProductCapabilityMetadataParser(),
                new DeviceCapabilityRegistry(),
                commandGateway
        );
    }

    @Test
    void returnsBroadcastCapabilitiesForRegisteredDevice() {
        Device device = device(10L, "B001", 1, 1, 1);
        Product product = product(10L, "broadcast-v1", """
                {"governance":{"productCapabilityType":"WARNING","warningDeviceKind":"BROADCAST"}}
                """);
        when(deviceService.getRequiredByCode(99L, "B001")).thenReturn(device);
        when(productService.getRequiredById(10L)).thenReturn(product);

        DeviceCapabilityOverviewVO overview = service.getCapabilities(99L, "B001");

        assertThat(overview.getProductCapabilityType()).isEqualTo("WARNING");
        assertThat(overview.getSubType()).isEqualTo("BROADCAST");
        assertThat(overview.getOnlineExecutable()).isTrue();
        assertThat(overview.getCapabilities()).extracting("code")
                .containsExactly("broadcast_play", "broadcast_stop", "broadcast_volume", "reboot");
    }

    @Test
    void disablesOnlineCapabilitiesWhenDeviceOffline() {
        Device device = device(10L, "B001", 0, 1, 1);
        Product product = product(10L, "broadcast-v1", """
                {"governance":{"productCapabilityType":"WARNING","warningDeviceKind":"BROADCAST"}}
                """);
        when(deviceService.getRequiredByCode(99L, "B001")).thenReturn(device);
        when(productService.getRequiredById(10L)).thenReturn(product);

        DeviceCapabilityOverviewVO overview = service.getCapabilities(99L, "B001");

        assertThat(overview.getOnlineExecutable()).isFalse();
        assertThat(overview.getCapabilities()).allSatisfy(item -> {
            assertThat(item.getEnabled()).isFalse();
            assertThat(item.getDisabledReason()).isEqualTo("设备离线，暂不可执行下行操作");
        });
    }

    @Test
    void rejectsUnregisteredDevice() {
        Device device = device(10L, "X001", 1, 1, 1);
        device.setRegistrationStatus(0);
        when(deviceService.getRequiredByCode(99L, "X001")).thenReturn(device);

        assertThatThrownBy(() -> service.getCapabilities(99L, "X001"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未登记设备不支持设备操作");
    }

    private Device device(Long productId, String deviceCode, Integer onlineStatus, Integer activateStatus, Integer deviceStatus) {
        Device device = new Device();
        device.setId(1L);
        device.setProductId(productId);
        device.setProductKey("broadcast-v1");
        device.setDeviceCode(deviceCode);
        device.setOnlineStatus(onlineStatus);
        device.setActivateStatus(activateStatus);
        device.setDeviceStatus(deviceStatus);
        device.setRegistrationStatus(1);
        return device;
    }

    private Product product(Long id, String productKey, String metadataJson) {
        Product product = new Product();
        product.setId(id);
        product.setProductKey(productKey);
        product.setProductName("广播预警设备");
        product.setProtocolCode("mqtt-json");
        product.setMetadataJson(metadataJson);
        product.setStatus(1);
        return product;
    }
}
```

- [ ] **Step 2: Run service test to verify it fails**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=DeviceCapabilityServiceImplTest test
```

Expected: compilation fails because service and VO classes do not exist.

- [ ] **Step 3: Implement VOs and service read path**

Create `DeviceCapabilityVO.java`:

```java
package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.util.Map;

@Data
public class DeviceCapabilityVO {
    private String code;
    private String name;
    private String group;
    private Boolean enabled;
    private Boolean requiresOnline;
    private String disabledReason;
    private Map<String, Map<String, Object>> paramsSchema;
}
```

Create `DeviceCapabilityOverviewVO.java`:

```java
package com.ghlzm.iot.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeviceCapabilityOverviewVO {
    private String deviceCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;
    private String productKey;
    private String productCapabilityType;
    private String subType;
    private Boolean onlineExecutable;
    private String disabledReason;
    private List<DeviceCapabilityVO> capabilities = new ArrayList<>();
}
```

Create `DeviceCapabilityService.java`:

```java
package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import com.ghlzm.iot.framework.mybatis.PageResult;

public interface DeviceCapabilityService {

    DeviceCapabilityOverviewVO getCapabilities(Long currentUserId, String deviceCode);

    DeviceCapabilityExecuteResultVO execute(Long currentUserId,
                                            String deviceCode,
                                            String capabilityCode,
                                            DeviceCapabilityExecuteDTO dto);

    PageResult<CommandRecordPageItemVO> pageCommands(Long currentUserId,
                                                     String deviceCode,
                                                     String capabilityCode,
                                                     String status,
                                                     Integer pageNum,
                                                     Integer pageSize);
}
```

Create `DeviceCapabilityServiceImpl.java` with read path and execute/page methods throwing `BizException("能力执行暂未接入")` until later tasks replace them:

```java
package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.DeviceCapabilityRegistry;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadata;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadataParser;
import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceCapabilityCommandGateway;
import com.ghlzm.iot.device.service.DeviceCapabilityService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityVO;
import com.ghlzm.iot.framework.mybatis.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceCapabilityServiceImpl implements DeviceCapabilityService {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final ProductCapabilityMetadataParser metadataParser;
    private final DeviceCapabilityRegistry capabilityRegistry;
    private final DeviceCapabilityCommandGateway commandGateway;

    public DeviceCapabilityServiceImpl(DeviceService deviceService,
                                       ProductService productService,
                                       ProductCapabilityMetadataParser metadataParser,
                                       DeviceCapabilityRegistry capabilityRegistry,
                                       DeviceCapabilityCommandGateway commandGateway) {
        this.deviceService = deviceService;
        this.productService = productService;
        this.metadataParser = metadataParser;
        this.capabilityRegistry = capabilityRegistry;
        this.commandGateway = commandGateway;
    }

    @Override
    public DeviceCapabilityOverviewVO getCapabilities(Long currentUserId, String deviceCode) {
        Device device = deviceService.getRequiredByCode(currentUserId, deviceCode);
        ensureRegistered(device);
        Product product = productService.getRequiredById(device.getProductId());
        ProductCapabilityMetadata metadata = metadataParser.parse(product.getMetadataJson());

        String disabledReason = resolveDeviceDisabledReason(device, product);
        boolean executable = disabledReason == null;
        List<DeviceCapabilityDefinition> definitions = capabilityRegistry.resolve(metadata);

        DeviceCapabilityOverviewVO vo = new DeviceCapabilityOverviewVO();
        vo.setDeviceCode(device.getDeviceCode());
        vo.setProductId(product.getId());
        vo.setProductKey(product.getProductKey());
        vo.setProductCapabilityType(metadata.capabilityType().name());
        vo.setSubType(resolveSubType(metadata));
        vo.setOnlineExecutable(executable);
        vo.setDisabledReason(disabledReason);
        vo.setCapabilities(definitions.stream().map(item -> toVO(item, executable, disabledReason)).toList());
        return vo;
    }

    @Override
    public DeviceCapabilityExecuteResultVO execute(Long currentUserId,
                                                  String deviceCode,
                                                  String capabilityCode,
                                                  DeviceCapabilityExecuteDTO dto) {
        throw new BizException("能力执行暂未接入");
    }

    @Override
    public PageResult<CommandRecordPageItemVO> pageCommands(Long currentUserId,
                                                           String deviceCode,
                                                           String capabilityCode,
                                                           String status,
                                                           Integer pageNum,
                                                           Integer pageSize) {
        throw new BizException("命令台账暂未接入");
    }

    private void ensureRegistered(Device device) {
        if (device == null || Integer.valueOf(0).equals(device.getRegistrationStatus())) {
            throw new BizException("未登记设备不支持设备操作");
        }
    }

    private String resolveDeviceDisabledReason(Device device, Product product) {
        if (product != null && Integer.valueOf(0).equals(product.getStatus())) {
            return "产品已停用，暂不可执行下行操作";
        }
        if (Integer.valueOf(0).equals(device.getDeviceStatus())) {
            return "设备已停用，暂不可执行下行操作";
        }
        if (Integer.valueOf(0).equals(device.getActivateStatus())) {
            return "设备未激活，暂不可执行下行操作";
        }
        if (Integer.valueOf(0).equals(device.getOnlineStatus())) {
            return "设备离线，暂不可执行下行操作";
        }
        return null;
    }

    private String resolveSubType(ProductCapabilityMetadata metadata) {
        return switch (metadata.capabilityType()) {
            case WARNING -> metadata.warningDeviceKind().name();
            case VIDEO -> metadata.videoDeviceKind().name();
            default -> null;
        };
    }

    private DeviceCapabilityVO toVO(DeviceCapabilityDefinition definition, boolean executable, String disabledReason) {
        DeviceCapabilityVO vo = new DeviceCapabilityVO();
        vo.setCode(definition.code());
        vo.setName(definition.name());
        vo.setGroup(definition.group());
        vo.setRequiresOnline(definition.requiresOnline());
        vo.setEnabled(executable);
        vo.setDisabledReason(executable ? null : disabledReason);
        vo.setParamsSchema(definition.paramsSchema());
        return vo;
    }
}
```

- [ ] **Step 4: Add gateway interface and model classes**

Create `DeviceCapabilityCommandGateway.java`:

```java
package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;

public interface DeviceCapabilityCommandGateway {

    DeviceCapabilityCommandResult publish(DeviceCapabilityCommandRequest request);
}
```

Create gateway model classes:

```java
package com.ghlzm.iot.device.service.model;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import lombok.Data;

import java.util.Map;

@Data
public class DeviceCapabilityCommandRequest {
    private Device device;
    private Product product;
    private String capabilityCode;
    private Map<String, Object> params;
}
```

```java
package com.ghlzm.iot.device.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceCapabilityCommandResult {
    private String commandId;
    private String topic;
    private Integer qos;
    private Boolean retained;
    private String status;
}
```

- [ ] **Step 5: Run service test**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=DeviceCapabilityServiceImplTest test
```

Expected: tests pass.

- [ ] **Step 6: Add controller and controller test**

Create `DeviceCapabilityController.java`:

```java
package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.service.DeviceCapabilityService;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import com.ghlzm.iot.framework.mybatis.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceCapabilityController {

    private final DeviceCapabilityService deviceCapabilityService;

    public DeviceCapabilityController(DeviceCapabilityService deviceCapabilityService) {
        this.deviceCapabilityService = deviceCapabilityService;
    }

    @GetMapping("/api/device/{deviceCode}/capabilities")
    public R<DeviceCapabilityOverviewVO> getCapabilities(@PathVariable String deviceCode,
                                                         Authentication authentication) {
        return R.ok(deviceCapabilityService.getCapabilities(requireCurrentUserId(authentication), deviceCode));
    }

    @PostMapping("/api/device/{deviceCode}/capabilities/{capabilityCode}/execute")
    public R<DeviceCapabilityExecuteResultVO> execute(@PathVariable String deviceCode,
                                                      @PathVariable String capabilityCode,
                                                      @RequestBody DeviceCapabilityExecuteDTO dto,
                                                      Authentication authentication) {
        return R.ok(deviceCapabilityService.execute(requireCurrentUserId(authentication), deviceCode, capabilityCode, dto));
    }

    @GetMapping("/api/device/{deviceCode}/commands")
    public R<PageResult<CommandRecordPageItemVO>> pageCommands(@PathVariable String deviceCode,
                                                               @RequestParam(required = false) String capabilityCode,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                               Authentication authentication) {
        return R.ok(deviceCapabilityService.pageCommands(
                requireCurrentUserId(authentication),
                deviceCode,
                capabilityCode,
                status,
                pageNum,
                pageSize
        ));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
```

Create `DeviceCapabilityControllerTest.java` using the same MockMvc style as existing controller tests in the module. Test `GET /api/device/B001/capabilities` returns service data and passes current user id.

- [ ] **Step 7: Run controller test**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -Dtest=DeviceCapabilityControllerTest test
```

Expected: controller test passes.

- [ ] **Step 8: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/capability spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCapabilityController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceCapabilityExecuteDTO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceCapabilityService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceCapabilityCommandGateway.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityOverviewVO.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImplTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceCapabilityControllerTest.java
git commit -m "feat: expose device capability read api"
```

---

### Task 4: Capability Command Payload Builders

**Files:**
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityCommandPayload.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/WarningCapabilityPayloadBuilder.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/VideoCapabilityPayloadBuilder.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/capability/WarningCapabilityPayloadBuilderTest.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/capability/VideoCapabilityPayloadBuilderTest.java`

- [ ] **Step 1: Write warning payload builder tests**

Create `WarningCapabilityPayloadBuilderTest.java`:

```java
package com.ghlzm.iot.message.service.capability;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarningCapabilityPayloadBuilderTest {

    private final WarningCapabilityPayloadBuilder builder = new WarningCapabilityPayloadBuilder();

    @Test
    void buildsBroadcastPlayPayload() {
        CapabilityCommandPayload payload = builder.build(
                "D001",
                "broadcast_play",
                "1776999000000",
                Map.of("content", "前方施工", "bNum", 1, "volume", 80)
        );

        assertThat(payload.topic()).isEqualTo("/iot/broadcast/D001");
        assertThat(new String(payload.payload(), StandardCharsets.UTF_8))
                .isEqualTo("$cmd=broadcast&b_num=1&b_size=4&b_content=前方施工&volume=80&msgid=1776999000000");
    }

    @Test
    void buildsBroadcastStopPayload() {
        CapabilityCommandPayload payload = builder.build("D001", "broadcast_stop", "1776999000000", Map.of());

        assertThat(payload.topic()).isEqualTo("/iot/broadcast/D001");
        assertThat(new String(payload.payload(), StandardCharsets.UTF_8))
                .isEqualTo("$cmd=stop&msgid=1776999000000");
    }

    @Test
    void buildsLedProgramPayload() {
        CapabilityCommandPayload payload = builder.build(
                "LED001",
                "led_program",
                "1776999000000",
                Map.of("type", 1, "brigh", 8, "freq", 4)
        );

        assertThat(payload.topic()).isEqualTo("/iot/led/LED001");
        assertThat(new String(payload.payload(), StandardCharsets.UTF_8))
                .isEqualTo("$cmd=led&type=1&brigh=8&freq=4&msgid=1776999000000");
    }

    @Test
    void buildsFlashControlPayload() {
        CapabilityCommandPayload payload = builder.build(
                "F001",
                "flash_control",
                "1776999000000",
                Map.of("type", 3, "brigh", 8, "freq", 4)
        );

        assertThat(payload.topic()).isEqualTo("/iot/flash/F001");
        assertThat(new String(payload.payload(), StandardCharsets.UTF_8))
                .isEqualTo("$cmd=flash&type=3&brigh=8&freq=4&msgid=1776999000000");
    }

    @Test
    void rejectsInvalidVolume() {
        assertThatThrownBy(() -> builder.build(
                "D001",
                "broadcast_volume",
                "1776999000000",
                Map.of("volume", 101)
        )).hasMessageContaining("音量范围为 0-100");
    }
}
```

- [ ] **Step 2: Write video payload builder tests**

Create `VideoCapabilityPayloadBuilderTest.java`:

```java
package com.ghlzm.iot.message.service.capability;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoCapabilityPayloadBuilderTest {

    private final VideoCapabilityPayloadBuilder builder = new VideoCapabilityPayloadBuilder();

    @Test
    void buildsVideoPlayPayloadAsSemanticJson() {
        CapabilityCommandPayload payload = builder.build(
                "V001",
                "video_play",
                "1776999000000",
                Map.of("streamUrl", "rtsp://example/video", "channel", "1")
        );

        assertThat(payload.topic()).isEqualTo("/iot/video/V001");
        assertThat(new String(payload.payload(), StandardCharsets.UTF_8))
                .contains("\"cmd\":\"video_play\"")
                .contains("\"msgid\":\"1776999000000\"")
                .contains("\"streamUrl\":\"rtsp://example/video\"");
    }

    @Test
    void buildsAzimuthTurnPayload() {
        CapabilityCommandPayload payload = builder.build(
                "V001",
                "video_turn_azimuth",
                "1776999000000",
                Map.of("azimuth", 180)
        );

        assertThat(payload.topic()).isEqualTo("/iot/video/V001");
        assertThat(new String(payload.payload(), StandardCharsets.UTF_8))
                .contains("\"cmd\":\"video_turn_azimuth\"")
                .contains("\"azimuth\":180");
    }

    @Test
    void rejectsInvalidAzimuth() {
        assertThatThrownBy(() -> builder.build(
                "V001",
                "video_turn_azimuth",
                "1776999000000",
                Map.of("azimuth", 361)
        )).hasMessageContaining("方位角范围为 0-360");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-message -Dtest=WarningCapabilityPayloadBuilderTest,VideoCapabilityPayloadBuilderTest test
```

Expected: compilation fails because payload builder classes do not exist.

- [ ] **Step 4: Implement payload classes**

Create `CapabilityCommandPayload.java`:

```java
package com.ghlzm.iot.message.service.capability;

public record CapabilityCommandPayload(
        String topic,
        byte[] payload,
        String commandType,
        String serviceIdentifier
) {
}
```

Create `WarningCapabilityPayloadBuilder.java` with explicit range validation and string payloads. Use `String.valueOf(content).length()` for `b_size`, matching the provided text-level protocol examples.

Create `VideoCapabilityPayloadBuilder.java` with semantic JSON payloads:

```json
{"cmd":"video_turn_azimuth","msgid":"1776999000000","params":{"azimuth":180}}
```

Use `tools.jackson.databind.json.JsonMapper` to serialize the JSON map so escaping is safe.

- [ ] **Step 5: Run builder tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-message -Dtest=WarningCapabilityPayloadBuilderTest,VideoCapabilityPayloadBuilderTest test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/capability
git commit -m "feat: build device capability command payloads"
```

---

### Task 5: Capability Command Execution Gateway

**Files:**
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/DeviceCapabilityDownCommandService.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityCommandGatewayImpl.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityDownCommandServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceCapabilityExecuteDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityExecuteResultVO.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityDownCommandServiceImplTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImplExecuteTest.java`

- [ ] **Step 1: Write execution service tests**

Test that `DeviceCapabilityDownCommandServiceImpl`:

- Creates `CommandRecord` with `commandId`, `deviceCode`, `productKey`, `topic`, `commandType=capability`, `serviceIdentifier=capabilityCode`, raw request payload.
- Publishes raw payload with `MqttDownMessagePublisher.publishRaw`.
- Marks command `SENT` after publish.
- Marks command `FAILED` and rethrows when publish fails.

Use Mockito to mock `MqttDownMessagePublisher`, `CommandRecordService`, and payload builders.

- [ ] **Step 2: Run execution service test to verify it fails**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-message -Dtest=DeviceCapabilityDownCommandServiceImplTest test
```

Expected: compilation fails because execution classes do not exist.

- [ ] **Step 3: Implement message gateway and execution service**

Create `DeviceCapabilityDownCommandService.java`:

```java
package com.ghlzm.iot.message.service;

import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;

public interface DeviceCapabilityDownCommandService {

    DeviceCapabilityCommandResult publish(DeviceCapabilityCommandRequest request);
}
```

Create `DeviceCapabilityCommandGatewayImpl.java`:

```java
package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.device.service.DeviceCapabilityCommandGateway;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;
import com.ghlzm.iot.message.service.DeviceCapabilityDownCommandService;
import org.springframework.stereotype.Component;

@Component
public class DeviceCapabilityCommandGatewayImpl implements DeviceCapabilityCommandGateway {

    private final DeviceCapabilityDownCommandService downCommandService;

    public DeviceCapabilityCommandGatewayImpl(DeviceCapabilityDownCommandService downCommandService) {
        this.downCommandService = downCommandService;
    }

    @Override
    public DeviceCapabilityCommandResult publish(DeviceCapabilityCommandRequest request) {
        return downCommandService.publish(request);
    }
}
```

Implement `DeviceCapabilityDownCommandServiceImpl`:

- Generate command id with `String.valueOf(System.currentTimeMillis())`.
- Choose warning builder for `broadcast_* / led_* / flash_* / reboot`.
- Choose video builder for `video_*`.
- For `reboot`, choose topic by product subtype if possible. Broadcast uses `/iot/broadcast/{deviceCode}`, LED uses `/iot/led/{deviceCode}`, flash uses `/iot/flash/{deviceCode}`.
- Use existing `CommandRecordService.create`, `markSent`, `markFailed`.
- Use `MqttDownMessagePublisher.publishRaw(topic, payload, qos, false)`.

- [ ] **Step 4: Implement execute DTO/VO and service execute path**

Create `DeviceCapabilityExecuteDTO.java`:

```java
package com.ghlzm.iot.device.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DeviceCapabilityExecuteDTO {
    private Map<String, Object> params = new HashMap<>();
}
```

Create `DeviceCapabilityExecuteResultVO.java`:

```java
package com.ghlzm.iot.device.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceCapabilityExecuteResultVO {
    private String commandId;
    private String deviceCode;
    private String capabilityCode;
    private String status;
    private String topic;
    private LocalDateTime sentAt;
}
```

Replace `DeviceCapabilityServiceImpl.execute`:

- Load device by current user.
- Ensure registered and executable.
- Load product.
- Parse metadata and require capability from registry.
- Reject unknown capability with `BizException("当前产品不支持该设备能力: " + capabilityCode)`.
- Build `DeviceCapabilityCommandRequest`.
- Call gateway.
- Map gateway result to VO.

- [ ] **Step 5: Run execution tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-message -Dtest=DeviceCapabilityServiceImplExecuteTest,DeviceCapabilityDownCommandServiceImplTest test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/DeviceCapabilityExecuteDTO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceCapabilityExecuteResultVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/DeviceCapabilityDownCommandService.java spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityCommandGatewayImpl.java spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityDownCommandServiceImpl.java spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/impl/DeviceCapabilityDownCommandServiceImplTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImplExecuteTest.java
git commit -m "feat: execute device capability commands"
```

---

### Task 6: Command Feedback Parser and Command List

**Files:**
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityFeedback.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityFeedbackParser.java`
- Create: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability/CapabilityFeedbackHandler.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CommandRecordService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CommandRecordServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CommandRecordPageItemVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/capability/CapabilityFeedbackParserTest.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/capability/CapabilityFeedbackHandlerTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/CommandRecordServiceImplTest.java`

- [ ] **Step 1: Write parser tests**

Create parser tests for:

- `$cmd=stop&result=sucd&message=停止播放&msgid=1776999000000`
- `$cmd=stop&result=fail&message=失败原因&msgid=1776999000000`
- malformed string returns invalid feedback with reason.

- [ ] **Step 2: Implement parser and handler**

Create `CapabilityFeedback.java`:

```java
package com.ghlzm.iot.message.service.capability;

public record CapabilityFeedback(
        boolean valid,
        String cmd,
        String result,
        String msgid,
        String message,
        String rawPayload,
        String invalidReason
) {
}
```

Parser rules:

- Strip leading `$` from `$cmd`.
- Split by `&`, then split each pair at first `=`.
- Require `cmd`, `result`, and `msgid`.
- Treat `sucd` as success and `fail` as failed in handler.

Handler rules:

- For `sucd`, call `CommandRecordService.markSuccessByCommandId(msgid, rawPayload, LocalDateTime.now())`.
- For `fail`, call `CommandRecordService.markFailedByCommandId(msgid, rawPayload, message, LocalDateTime.now())`.
- If command id not found, log a warning and do not throw.

- [ ] **Step 3: Implement command list**

Add to `CommandRecordService`:

```java
PageResult<CommandRecordPageItemVO> pageByDevice(Long currentUserId,
                                                 String deviceCode,
                                                 String capabilityCode,
                                                 String status,
                                                 Integer pageNum,
                                                 Integer pageSize);
```

Implementation:

- Use `DeviceService.getRequiredByCode(currentUserId, deviceCode)` first for data-scope validation.
- Query `iot_command_record` by `deviceCode`, optional `serviceIdentifier=capabilityCode`, optional `status`.
- Order by `createTime desc`.
- Map to `CommandRecordPageItemVO`.

- [ ] **Step 4: Wire `DeviceCapabilityServiceImpl.pageCommands`**

Inject `CommandRecordService` and delegate to `pageByDevice`.

- [ ] **Step 5: Run tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-message -Dtest=CapabilityFeedbackParserTest,CapabilityFeedbackHandlerTest,CommandRecordServiceImplTest test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/service/capability spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/service/capability spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CommandRecordService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CommandRecordServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CommandRecordPageItemVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceCapabilityServiceImpl.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/CommandRecordServiceImplTest.java
git commit -m "feat: handle device command feedback"
```

---

### Task 7: Feedback Topic Interception

**Files:**
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java` or the local topic-route stage class after inspection.
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java` or a focused feedback route test.

- [ ] **Step 1: Locate the earliest stable interception point**

Inspect the pipeline classes and choose the point where raw topic and raw payload are available before `PROTOCOL_DECODE` writes properties. Prefer a small dedicated component:

```java
public class CapabilityFeedbackTopicMatcher {
    public boolean matches(String topic) {
        return topic != null
                && (topic.matches("^/broadcast/[^/]+/feedback$")
                || topic.matches("^/iot/broadcast/[^/]+/feedback$")
                || topic.matches("^/iot/led/[^/]+/feedback$")
                || topic.matches("^/iot/flash/[^/]+/feedback$"));
    }
}
```

- [ ] **Step 2: Write route test**

Test that a feedback topic:

- Calls `CapabilityFeedbackHandler`.
- Produces a completed/non-property-processing result.
- Does not call regular protocol decode or payload apply mocks.

- [ ] **Step 3: Implement interception**

Add a small feedback branch:

- If topic matches feedback pattern, parse raw payload as UTF-8.
- Call feedback handler.
- Finish pipeline with a success summary containing `feedback=true`, `topic`, `msgid`.
- Do not write latest properties.

- [ ] **Step 4: Run pipeline tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-message -Dtest=UpMessageProcessingPipelineTest test
```

Expected: tests pass, including existing main pipeline tests.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-message/src/main/java/com/ghlzm/iot/message spring-boot-iot-message/src/test/java/com/ghlzm/iot/message
git commit -m "feat: route command feedback messages"
```

---

### Task 8: Frontend API Types and Capability Components

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/device.ts`
- Create: `spring-boot-iot-ui/src/components/device/DeviceCapabilityPanel.vue`
- Create: `spring-boot-iot-ui/src/components/device/DeviceCapabilityExecuteDrawer.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/device/DeviceCapabilityPanel.test.ts`

- [ ] **Step 1: Add TypeScript types**

Add to `types/api.ts`:

```ts
export type DeviceCapabilityType = 'COLLECTING' | 'MONITORING' | 'WARNING' | 'VIDEO' | 'UNKNOWN' | string

export interface DeviceCapability {
  code: string
  name: string
  group?: string | null
  enabled?: boolean | null
  requiresOnline?: boolean | null
  disabledReason?: string | null
  paramsSchema?: Record<string, Record<string, unknown>> | null
}

export interface DeviceCapabilityOverview {
  deviceCode: string
  productId?: IdType | null
  productKey?: string | null
  productCapabilityType?: DeviceCapabilityType | null
  subType?: string | null
  onlineExecutable?: boolean | null
  disabledReason?: string | null
  capabilities: DeviceCapability[]
}

export interface DeviceCapabilityExecutePayload {
  params: Record<string, unknown>
}

export interface DeviceCapabilityExecuteResult {
  commandId: string
  deviceCode: string
  capabilityCode: string
  status: string
  topic?: string | null
  sentAt?: string | null
}

export interface CommandRecordPageItem {
  id: IdType
  commandId?: string | null
  deviceCode?: string | null
  topic?: string | null
  commandType?: string | null
  serviceIdentifier?: string | null
  status?: string | null
  sendTime?: string | null
  ackTime?: string | null
  timeoutTime?: string | null
  errorMessage?: string | null
  replyPayload?: string | null
}
```

- [ ] **Step 2: Add API functions**

Add to `api/device.ts`:

```ts
export function getDeviceCapabilities(deviceCode: string): Promise<ApiEnvelope<DeviceCapabilityOverview>> {
  return request<DeviceCapabilityOverview>(`/api/device/${deviceCode}/capabilities`)
}

export function executeDeviceCapability(
  deviceCode: string,
  capabilityCode: string,
  payload: DeviceCapabilityExecutePayload
): Promise<ApiEnvelope<DeviceCapabilityExecuteResult>> {
  return request<DeviceCapabilityExecuteResult>(`/api/device/${deviceCode}/capabilities/${capabilityCode}/execute`, {
    method: 'POST',
    body: payload
  })
}

export function pageDeviceCommands(
  deviceCode: string,
  params: { capabilityCode?: string; status?: string; pageNum?: number; pageSize?: number } = {}
): Promise<ApiEnvelope<PageResult<CommandRecordPageItem>>> {
  const query = buildQuery(params)
  return request<PageResult<CommandRecordPageItem>>(`/api/device/${deviceCode}/commands${query ? `?${query}` : ''}`)
}
```

- [ ] **Step 3: Write component test**

Test `DeviceCapabilityPanel`:

- Renders capability type and subtype.
- Groups `broadcast_play` under `广播预警`.
- Emits `execute` when enabled capability clicked.
- Shows disabled reason for offline device.

- [ ] **Step 4: Implement `DeviceCapabilityPanel.vue`**

Use existing shared visual language:

- `StandardInlineState` for disabled overview message.
- `StandardTableToolbar` or compact local header for command list meta.
- `StandardButton` for capability actions.
- No nested cards inside cards.
- No private blue/orange/purple palettes.

Props:

```ts
const props = defineProps<{
  overview: DeviceCapabilityOverview | null
  commands: CommandRecordPageItem[]
  loading?: boolean
  commandLoading?: boolean
}>()
const emit = defineEmits<{
  execute: [capability: DeviceCapability]
  refreshCommands: []
}>()
```

- [ ] **Step 5: Implement `DeviceCapabilityExecuteDrawer.vue`**

Use `StandardFormDrawer` + `StandardDrawerFooter`.

Rules:

- Render string schema as `el-input`.
- Render integer schema as `el-input-number`.
- Apply required/min/max validation from `paramsSchema`.
- Submit emits `{ capability, params }`.

- [ ] **Step 6: Run frontend tests**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- DeviceCapabilityPanel
```

Expected: tests pass.

- [ ] **Step 7: Commit**

```bash
git add spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/api/device.ts spring-boot-iot-ui/src/components/device/DeviceCapabilityPanel.vue spring-boot-iot-ui/src/components/device/DeviceCapabilityExecuteDrawer.vue spring-boot-iot-ui/src/__tests__/components/device/DeviceCapabilityPanel.test.ts
git commit -m "feat: add device capability frontend components"
```

---

### Task 9: Integrate Capability UI Into Device Asset Center

**Files:**
- Modify: `spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: Update detail workbench props**

Add props:

```ts
const props = defineProps<{
  device: Device
  capabilityOverview?: DeviceCapabilityOverview | null
  commandRecords?: CommandRecordPageItem[]
  capabilityLoading?: boolean
  commandLoading?: boolean
}>()
```

Add emits:

```ts
const emit = defineEmits<{
  executeCapability: [capability: DeviceCapability]
  refreshCommands: []
}>()
```

Render `DeviceCapabilityPanel` only when `isRegistered`.

- [ ] **Step 2: Update DeviceWorkbenchView state**

Add state:

```ts
const capabilityOverview = ref<DeviceCapabilityOverview | null>(null)
const commandRecords = ref<CommandRecordPageItem[]>([])
const capabilityLoading = ref(false)
const commandLoading = ref(false)
const capabilityExecuteVisible = ref(false)
const executingCapability = ref<DeviceCapability | null>(null)
```

When `openDetail(row)` succeeds for a registered row:

- Call `deviceApi.getDeviceCapabilities(deviceCode)`.
- Call `deviceApi.pageDeviceCommands(deviceCode, { pageNum: 1, pageSize: 10 })`.
- Use request-id guard, matching existing detail request style.

- [ ] **Step 3: Add row menu entry**

In `getDeviceRowActions(row)`, add `设备操作` for registered devices with permission `iot:device-capability:view`. Command opens detail and scrolls/focuses capability section. If scroll focus is too invasive for first pass, simply opening detail is acceptable.

- [ ] **Step 4: Wire execute drawer**

When panel emits `execute`, open `DeviceCapabilityExecuteDrawer`.

On submit:

- Call `deviceApi.executeDeviceCapability(detailData.deviceCode, capability.code, { params })`.
- Show success message: `指令已下发，等待设备反馈：${commandId}`.
- Refresh commands.

- [ ] **Step 5: Run UI tests**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- DeviceDetailWorkbench DeviceWorkbenchView
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: show device capabilities in asset center"
```

---

### Task 10: Documentation and Quality Gates

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Check/possibly modify: `README.md`
- Check/possibly modify: `AGENTS.md`

- [ ] **Step 1: Update business docs**

Add to `docs/02-业务功能与流程说明.md` device asset section:

- Product metadata `governance.productCapabilityType` drives capabilities.
- `WARNING` subtypes: `BROADCAST / LED / FLASH`.
- `VIDEO` first slice: play, stop, azimuth turn.
- Command status lifecycle.

- [ ] **Step 2: Update API docs**

Add to `docs/03-接口规范与接口清单.md`:

- `GET /api/device/{deviceCode}/capabilities`
- `POST /api/device/{deviceCode}/capabilities/{capabilityCode}/execute`
- `GET /api/device/{deviceCode}/commands`
- Request/response examples.

- [ ] **Step 3: Update test docs**

Add to `docs/05-自动化测试与质量保障.md`:

- Capability registry unit tests.
- Payload builder unit tests.
- Feedback parser tests.
- UI component tests.
- Real environment MQTT feedback validation.

- [ ] **Step 4: Check README and AGENTS**

Run:

```bash
git diff -- README.md AGENTS.md
```

If they already contain unrelated user changes, do not rewrite them. Only add a short note if the new capability is part of the current baseline.

- [ ] **Step 5: Run backend focused tests**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-message -Dtest=ProductCapabilityMetadataParserTest,DeviceCapabilityRegistryTest,DeviceCapabilityServiceImplTest,DeviceCapabilityServiceImplExecuteTest,DeviceCapabilityDownCommandServiceImplTest,CapabilityFeedbackParserTest,CapabilityFeedbackHandlerTest test
```

Expected: all selected tests pass.

- [ ] **Step 6: Run frontend tests and guards**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- DeviceCapabilityPanel DeviceDetailWorkbench DeviceWorkbenchView
npm run component:guard
npm run list:guard
```

Expected: tests and guards pass.

- [ ] **Step 7: Run build smoke**

Run:

```bash
mvn -s .mvn/settings.xml -pl spring-boot-iot-device,spring-boot-iot-message -am -DskipTests package
```

Expected: modules compile.

- [ ] **Step 8: Commit docs and final verification**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/05-自动化测试与质量保障.md README.md AGENTS.md
git commit -m "docs: document device capability operations"
```

If `README.md` or `AGENTS.md` had no relevant changes, do not stage them.

---

## Final Verification Checklist

- [ ] Branch is `codex/dev`.
- [ ] No unrelated user changes were reverted.
- [ ] Device capabilities come from product metadata, not device-local type fields.
- [ ] Warning subtypes render correct capability sets.
- [ ] Video PTZ exposes play, stop, and azimuth turn only.
- [ ] Command execution creates `iot_command_record` before publish.
- [ ] Feedback updates command state by `msgid`.
- [ ] Device asset center uses shared row action and drawer components.
- [ ] Docs are updated in place.
- [ ] Focused Maven tests pass.
- [ ] Focused frontend tests and guards pass.
