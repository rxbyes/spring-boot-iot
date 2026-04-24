package com.ghlzm.iot.device.capability;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DeviceCapabilityRegistry {

    private static final String GROUP_MAINTENANCE = "基础维护";
    private static final String GROUP_BROADCAST = "广播预警";
    private static final String GROUP_LED = "情报板";
    private static final String GROUP_FLASH = "爆闪灯";
    private static final String GROUP_VIDEO = "视频控制";

    private static final List<DeviceCapabilityDefinition> MONITORING_CAPABILITIES = List.of(
            definition(
                    DeviceCapabilityCode.POWER_SWITCH,
                    "开关",
                    GROUP_MAINTENANCE,
                    true,
                    Map.of("enabled", integerParam("开关状态", true, 0, 1))
            ),
            definition(DeviceCapabilityCode.REBOOT, "重启", GROUP_MAINTENANCE, true, Map.of()),
            definition(
                    DeviceCapabilityCode.FIRMWARE_UPGRADE,
                    "固件升级",
                    GROUP_MAINTENANCE,
                    true,
                    Map.of("version", stringParam("目标版本", true))
            )
    );

    private static final List<DeviceCapabilityDefinition> WARNING_BROADCAST_CAPABILITIES = List.of(
            definition(
                    DeviceCapabilityCode.BROADCAST_PLAY,
                    "播放内容",
                    GROUP_BROADCAST,
                    true,
                    Map.of(
                            "content", stringParam("播报内容", true),
                            "bNum", integerParam("播报次数", false, -1, 999),
                            "volume", integerParam("音量", false, 0, 100)
                    )
            ),
            definition(DeviceCapabilityCode.BROADCAST_STOP, "停止播放", GROUP_BROADCAST, true, Map.of()),
            definition(
                    DeviceCapabilityCode.BROADCAST_VOLUME,
                    "音量控制",
                    GROUP_BROADCAST,
                    true,
                    Map.of("volume", integerParam("音量", true, 0, 100))
            ),
            definition(DeviceCapabilityCode.REBOOT, "重启", GROUP_MAINTENANCE, true, Map.of())
    );

    private static final List<DeviceCapabilityDefinition> WARNING_LED_CAPABILITIES = List.of(
            definition(
                    DeviceCapabilityCode.LED_PROGRAM,
                    "节目控制",
                    GROUP_LED,
                    true,
                    Map.of(
                            "type", integerParam("节目编号", true, 1, 10),
                            "brigh", integerParam("亮度", true, 1, 8),
                            "freq", integerParam("频次", true, 1, 4)
                    )
            ),
            definition(DeviceCapabilityCode.LED_STOP, "关闭内容", GROUP_LED, true, Map.of()),
            definition(DeviceCapabilityCode.REBOOT, "重启", GROUP_MAINTENANCE, true, Map.of())
    );

    private static final List<DeviceCapabilityDefinition> WARNING_FLASH_CAPABILITIES = List.of(
            definition(
                    DeviceCapabilityCode.FLASH_CONTROL,
                    "爆闪控制",
                    GROUP_FLASH,
                    true,
                    Map.of(
                            "type", integerParam("灯控类型", true, 0, 3),
                            "brigh", integerParam("亮度", true, 1, 8),
                            "freq", integerParam("频次", true, 1, 4)
                    )
            ),
            definition(DeviceCapabilityCode.FLASH_STOP, "关闭设备", GROUP_FLASH, true, Map.of()),
            definition(DeviceCapabilityCode.REBOOT, "重启", GROUP_MAINTENANCE, true, Map.of())
    );

    private static final List<DeviceCapabilityDefinition> VIDEO_FIXED_CAPABILITIES = List.of(
            definition(
                    DeviceCapabilityCode.VIDEO_PLAY,
                    "播放视频",
                    GROUP_VIDEO,
                    true,
                    Map.of(
                            "streamUrl", stringParam("播放地址", false),
                            "channel", stringParam("通道", false),
                            "durationSeconds", integerParam("播放时长", false, 1, 86400)
                    )
            ),
            definition(
                    DeviceCapabilityCode.VIDEO_STOP,
                    "停止播放视频",
                    GROUP_VIDEO,
                    true,
                    Map.of("channel", stringParam("通道", false))
            )
    );

    private static final List<DeviceCapabilityDefinition> VIDEO_PTZ_CAPABILITIES = List.of(
            VIDEO_FIXED_CAPABILITIES.get(0),
            VIDEO_FIXED_CAPABILITIES.get(1),
            definition(
                    DeviceCapabilityCode.VIDEO_TURN_AZIMUTH,
                    "按方位角转向",
                    GROUP_VIDEO,
                    true,
                    Map.of("azimuth", integerParam("方位角", true, 0, 360))
            )
    );

    public List<DeviceCapabilityDefinition> resolve(ProductCapabilityMetadata metadata) {
        ProductCapabilityMetadata actual = metadata == null ? ProductCapabilityMetadata.unknown() : metadata;
        return switch (actual.capabilityType()) {
            case MONITORING -> MONITORING_CAPABILITIES;
            case WARNING -> resolveWarning(actual.warningDeviceKind());
            case VIDEO -> resolveVideo(actual.videoDeviceKind());
            case COLLECTING, UNKNOWN -> List.of();
        };
    }

    public DeviceCapabilityDefinition require(String capabilityCode, ProductCapabilityMetadata metadata) {
        if (capabilityCode == null || capabilityCode.isBlank()) {
            return null;
        }
        return resolve(metadata).stream()
                .filter(item -> item.code().equals(capabilityCode))
                .findFirst()
                .orElse(null);
    }

    private static List<DeviceCapabilityDefinition> resolveWarning(WarningDeviceKind kind) {
        return switch (kind) {
            case BROADCAST -> WARNING_BROADCAST_CAPABILITIES;
            case LED -> WARNING_LED_CAPABILITIES;
            case FLASH -> WARNING_FLASH_CAPABILITIES;
            case UNKNOWN -> List.of();
        };
    }

    private static List<DeviceCapabilityDefinition> resolveVideo(VideoDeviceKind kind) {
        return switch (kind) {
            case PTZ_CAMERA -> VIDEO_PTZ_CAPABILITIES;
            case FIXED_CAMERA -> VIDEO_FIXED_CAPABILITIES;
            case UNKNOWN -> List.of();
        };
    }

    private static DeviceCapabilityDefinition definition(String code,
                                                         String name,
                                                         String group,
                                                         boolean requiresOnline,
                                                         Map<String, Map<String, Object>> paramsSchema) {
        return new DeviceCapabilityDefinition(code, name, group, requiresOnline, paramsSchema);
    }

    private static Map<String, Object> stringParam(String label, boolean required) {
        return Map.of(
                "type", "string",
                "label", label,
                "required", required
        );
    }

    private static Map<String, Object> integerParam(String label, boolean required, int min, int max) {
        return Map.of(
                "type", "integer",
                "label", label,
                "required", required,
                "min", min,
                "max", max
        );
    }
}
