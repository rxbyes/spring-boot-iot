package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceDeletedFilterRegressionTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionService permissionService;

    private UserServiceImpl userService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        userService = spy(new UserServiceImpl(userMapper, passwordEncoder, permissionService));
    }

    @Test
    void shouldExcludeLogicDeletedUsersFromPagedQuery() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.ALL, true));

        Page<User> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(userService).page(any(Page.class), any(LambdaQueryWrapper.class));

        userService.pageUsers(1L, null, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(userService).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("deleted"));
    }
}
