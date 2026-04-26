package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityEvidenceQueryServiceImplContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldCreateServiceBeanFromSpringContext() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ObservabilityEvidenceQueryServiceImpl.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ObservabilityEvidenceQueryServiceImpl.class)
    static class TestConfig {

        @Bean
        JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate() {
                @Override
                public void afterPropertiesSet() {
                }
            };
        }

        @Bean
        PermissionService permissionService() {
            return new PermissionService() {
                @Override
                public UserAuthContextVO getUserAuthContext(Long userId) {
                    return null;
                }

                @Override
                public DataPermissionContext getDataPermissionContext(Long userId) {
                    return new DataPermissionContext(userId, 1L, null, null, true);
                }

                @Override
                public Set<Long> listAccessibleOrganizationIds(Long userId) {
                    return Collections.emptySet();
                }

                @Override
                public Set<Long> listWritableOrganizationIds(Long userId) {
                    return Collections.emptySet();
                }

                @Override
                public List<MenuTreeNodeVO> listMenuTree() {
                    return Collections.emptyList();
                }

                @Override
                public List<MenuTreeNodeVO> listMenuTree(Long currentUserId) {
                    return Collections.emptyList();
                }

                @Override
                public List<Long> listRoleMenuIds(Long roleId) {
                    return Collections.emptyList();
                }

                @Override
                public void replaceRoleMenus(Long roleId, List<Long> menuIds, Long operatorId) {
                }

                @Override
                public List<Long> listUserRoleIds(Long userId) {
                    return Collections.emptyList();
                }

                @Override
                public Map<Long, List<RoleSummaryVO>> listUserRolesByUserIds(Collection<Long> userIds) {
                    return Collections.emptyMap();
                }

                @Override
                public void replaceUserRoles(Long userId, List<Long> roleIds, Long operatorId) {
                }

                @Override
                public void deleteUserRoles(Long userId) {
                }

                @Override
                public void deleteRoleRelations(Long roleId) {
                }
            };
        }
    }
}
