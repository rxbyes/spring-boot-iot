package com.ghlzm.iot.protocol.mqtt.legacy.template.replay;

import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateReplayService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyDpTemplateReplayServiceContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ComponentScanConfig.class);

    @Test
    void registersReplayServiceBeanThroughComponentScan() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(ProtocolTemplateReplayService.class));
    }

    @Configuration
    @ComponentScan(basePackageClasses = LegacyDpTemplateReplayServiceImpl.class)
    static class ComponentScanConfig {
    }
}
