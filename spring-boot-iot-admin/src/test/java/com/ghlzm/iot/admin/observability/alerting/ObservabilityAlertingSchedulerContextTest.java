package com.ghlzm.iot.admin.observability.alerting;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityAlertingSchedulerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateSchedulerBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ObservabilityAlertingScheduler.class);
        });
    }

    @Test
    void shouldSkipSchedulerBeanWhenSchedulingDisabled() {
        contextRunner.withPropertyValues("iot.scheduling.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ObservabilityAlertingScheduler.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ObservabilityAlertingScheduler.class, IotProperties.class})
    static class TestConfig {

        @Bean
        ObservabilityAlertingService observabilityAlertingService() {
            return new ObservabilityAlertingService(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new IotProperties(),
                    java.time.Clock.systemDefaultZone()
            );
        }
    }
}
