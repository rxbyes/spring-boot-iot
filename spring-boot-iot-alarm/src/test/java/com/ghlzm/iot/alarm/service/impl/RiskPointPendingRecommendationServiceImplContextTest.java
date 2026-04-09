package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RiskPointPendingRecommendationServiceImplContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateRecommendationServiceBeanInSpringContext() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RiskPointPendingRecommendationServiceImpl.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(RiskPointPendingRecommendationServiceImpl.class)
    static class TestConfig {

        @Bean
        RiskPointPendingBindingService riskPointPendingBindingService() {
            return mock(RiskPointPendingBindingService.class);
        }

        @Bean
        DeviceService deviceService() {
            return mock(DeviceService.class);
        }

        @Bean
        ProductModelMapper productModelMapper() {
            return mock(ProductModelMapper.class);
        }

        @Bean
        DevicePropertyMapper devicePropertyMapper() {
            return mock(DevicePropertyMapper.class);
        }

        @Bean
        DeviceMessageLogMapper deviceMessageLogMapper() {
            return mock(DeviceMessageLogMapper.class);
        }

        @Bean
        RiskPointDevicePendingPromotionMapper riskPointDevicePendingPromotionMapper() {
            return mock(RiskPointDevicePendingPromotionMapper.class);
        }

        @Bean
        RiskMetricCatalogService riskMetricCatalogService() {
            return mock(RiskMetricCatalogService.class);
        }

        @Bean
        RiskMetricCatalogPublishRule riskMetricCatalogPublishRule() {
            return mock(RiskMetricCatalogPublishRule.class);
        }
    }
}
