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

        DeviceCapabilityDefinition playCapability = capabilities.stream()
                .filter(item -> item.code().equals("broadcast_play"))
                .findFirst()
                .orElseThrow();
        assertThat(playCapability.name()).isEqualTo("播放内容");
        assertThat(playCapability.group()).isEqualTo("广播预警");
        assertThat(playCapability.paramsSchema().get("content"))
                .containsEntry("type", "string")
                .containsEntry("label", "播报内容")
                .containsEntry("required", true);
        assertThat(playCapability.paramsSchema().get("bNum"))
                .containsEntry("label", "播报次数")
                .containsEntry("min", -1)
                .containsEntry("max", 999)
                .containsEntry("required", false);
        assertThat(playCapability.paramsSchema().get("volume"))
                .containsEntry("label", "音量")
                .containsEntry("min", 0)
                .containsEntry("max", 100)
                .containsEntry("required", false);
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
        DeviceCapabilityDefinition azimuthCapability = capabilities.stream()
                .filter(item -> item.code().equals("video_turn_azimuth"))
                .findFirst()
                .orElseThrow();
        assertThat(azimuthCapability.name()).isEqualTo("按方位角转向");
        assertThat(azimuthCapability.group()).isEqualTo("视频控制");
        assertThat(azimuthCapability.paramsSchema().get("azimuth"))
                .containsEntry("label", "方位角")
                .containsEntry("min", 0)
                .containsEntry("max", 360)
                .containsEntry("required", true);
    }

    @Test
    void fixedVideoDevicesExposePlayAndStopOnly() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.VIDEO, WarningDeviceKind.UNKNOWN, VideoDeviceKind.FIXED_CAMERA)
        );

        assertThat(codes(capabilities)).containsExactly("video_play", "video_stop");
    }

    @Test
    void collectingDevicesDoNotExposeControlCapabilitiesByDefault() {
        List<DeviceCapabilityDefinition> capabilities = registry.resolve(
                new ProductCapabilityMetadata(DeviceCapabilityType.COLLECTING, WarningDeviceKind.UNKNOWN, VideoDeviceKind.UNKNOWN)
        );

        assertThat(capabilities).isEmpty();
    }

    @Test
    void nullMetadataFallsBackToUnknownCapabilities() {
        assertThat(registry.resolve(null)).isEmpty();
    }

    @Test
    void requireReturnsMatchingDefinitionOrNull() {
        ProductCapabilityMetadata metadata = new ProductCapabilityMetadata(
                DeviceCapabilityType.MONITORING,
                WarningDeviceKind.UNKNOWN,
                VideoDeviceKind.UNKNOWN
        );

        DeviceCapabilityDefinition definition = registry.require("power_switch", metadata);

        assertThat(definition).isNotNull();
        assertThat(definition.name()).isEqualTo("开关");
        assertThat(definition.group()).isEqualTo("基础维护");
        assertThat(definition.paramsSchema().get("enabled"))
                .containsEntry("type", "integer")
                .containsEntry("label", "开关状态")
                .containsEntry("required", true)
                .containsEntry("min", 0)
                .containsEntry("max", 1);
        assertThat(registry.require("missing", metadata)).isNull();
        assertThat(registry.require(null, metadata)).isNull();
        assertThat(registry.require(" ", metadata)).isNull();
        assertThat(registry.require("power_switch", null)).isNull();
    }

    private List<String> codes(List<DeviceCapabilityDefinition> capabilities) {
        return capabilities.stream().map(DeviceCapabilityDefinition::code).toList();
    }
}
