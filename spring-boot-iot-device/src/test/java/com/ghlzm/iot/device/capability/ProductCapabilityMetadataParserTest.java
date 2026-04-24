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
