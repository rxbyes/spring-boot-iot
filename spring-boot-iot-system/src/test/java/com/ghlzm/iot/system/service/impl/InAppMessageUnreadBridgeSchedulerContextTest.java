package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.service.InAppMessageUnreadBridgeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InAppMessageUnreadBridgeSchedulerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateSchedulerBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(InAppMessageUnreadBridgeScheduler.class);
        });
    }

    @Test
    void shouldSkipSchedulerBeanWhenSchedulingDisabled() {
        contextRunner.withPropertyValues("iot.scheduling.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(InAppMessageUnreadBridgeScheduler.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({InAppMessageUnreadBridgeScheduler.class, IotProperties.class})
    static class TestConfig {

        @Bean
        InAppMessageUnreadBridgeService inAppMessageUnreadBridgeService() {
            return mock(InAppMessageUnreadBridgeService.class);
        }
    }
}
