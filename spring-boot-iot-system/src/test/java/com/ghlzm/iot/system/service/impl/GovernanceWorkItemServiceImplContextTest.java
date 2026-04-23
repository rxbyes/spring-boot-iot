package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.mapper.GovernanceReplayFeedbackMapper;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.GovernancePriorityScorer;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GovernanceWorkItemServiceImplContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateServiceBeanWhenContributorBeansAreAbsent() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GovernanceWorkItemServiceImpl.class);
            assertThat(context).hasSingleBean(GovernanceWorkItemService.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(GovernanceWorkItemServiceImpl.class)
    static class TestConfig {

        @Bean
        GovernanceWorkItemMapper governanceWorkItemMapper() {
            return mock(GovernanceWorkItemMapper.class);
        }

        @Bean
        GovernanceReplayFeedbackMapper governanceReplayFeedbackMapper() {
            return mock(GovernanceReplayFeedbackMapper.class);
        }

        @Bean
        GovernancePriorityScorer governancePriorityScorer() {
            return new GovernancePriorityScorerImpl();
        }

        @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
        Executor applicationTaskExecutor() {
            return Runnable::run;
        }
    }
}
