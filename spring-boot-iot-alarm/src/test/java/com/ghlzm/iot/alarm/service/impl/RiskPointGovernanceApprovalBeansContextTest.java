package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.governance.RiskPointGovernanceApprovalExecutor;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceCapabilityBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RiskPointGovernanceApprovalBeansContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateRiskPointGovernanceApprovalBeansInSpringContext() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RiskPointBindingMaintenanceServiceImpl.class);
            assertThat(context).hasSingleBean(RiskPointPendingPromotionServiceImpl.class);
            assertThat(context).hasSingleBean(RiskPointGovernanceApprovalExecutor.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            RiskPointBindingMaintenanceServiceImpl.class,
            RiskPointPendingPromotionServiceImpl.class,
            RiskPointGovernanceApprovalExecutor.class
    })
    static class TestConfig {

        @Bean
        RiskPointService riskPointService() {
            return mock(RiskPointService.class);
        }

        @Bean
        RiskPointDeviceMapper riskPointDeviceMapper() {
            return mock(RiskPointDeviceMapper.class);
        }

        @Bean
        RiskPointDeviceCapabilityBindingMapper riskPointDeviceCapabilityBindingMapper() {
            return mock(RiskPointDeviceCapabilityBindingMapper.class);
        }

        @Bean
        RiskPointDevicePendingBindingMapper riskPointDevicePendingBindingMapper() {
            return mock(RiskPointDevicePendingBindingMapper.class);
        }

        @Bean
        RiskPointDevicePendingPromotionMapper riskPointDevicePendingPromotionMapper() {
            return mock(RiskPointDevicePendingPromotionMapper.class);
        }

        @Bean
        RiskPointPendingRecommendationService riskPointPendingRecommendationService() {
            return mock(RiskPointPendingRecommendationService.class);
        }

        @Bean
        GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver() {
            return mock(GovernanceApprovalPolicyResolver.class);
        }

        @Bean
        GovernanceApprovalService governanceApprovalService() {
            return mock(GovernanceApprovalService.class);
        }

        @Bean
        GovernanceWorkItemService governanceWorkItemService() {
            return mock(GovernanceWorkItemService.class);
        }

        @Bean
        DeviceService deviceService() {
            return mock(DeviceService.class);
        }
    }
}
