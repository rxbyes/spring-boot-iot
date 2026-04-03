package com.ghlzm.iot.report.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class AutomationResultQueryServiceImplContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("iot.automation.results-dir=logs/acceptance")
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateServiceBeanFromConfiguredResultsDir() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AutomationResultQueryServiceImpl.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AutomationResultQueryServiceImpl.class)
    static class TestConfig {
    }
}
