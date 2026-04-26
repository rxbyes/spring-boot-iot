package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DeviceSessionTimeoutSchedulerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateSchedulerBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DeviceSessionTimeoutScheduler.class);
        });
    }

    @Test
    void shouldSkipSchedulerBeanWhenSchedulingDisabled() {
        contextRunner.withPropertyValues("iot.scheduling.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DeviceSessionTimeoutScheduler.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({DeviceSessionTimeoutScheduler.class, IotProperties.class})
    static class TestConfig {

        @Bean
        DeviceMapper deviceMapper() {
            return mock(DeviceMapper.class);
        }

        @Bean
        DeviceSessionService deviceSessionService() {
            return mock(DeviceSessionService.class);
        }

        @Bean
        DeviceOfflineTimeoutLeadershipService deviceOfflineTimeoutLeadershipService() {
            return mock(DeviceOfflineTimeoutLeadershipService.class);
        }
    }
}
