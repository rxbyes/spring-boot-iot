package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.RegionMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionServiceImplTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private RegionMapper regionMapper;

    private RegionServiceImpl regionService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Region.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        regionService = spy(new RegionServiceImpl(permissionService));
        Field field = findField(regionService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(regionService, regionMapper);
    }

    @Test
    void shouldFilterScopedRegionPageToCurrentTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        Page<Region> page = new Page<>(1L, 10L);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(regionService).page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class));

        invokeScopedPageRegions(99L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Region>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(regionService).page(org.mockito.ArgumentMatchers.any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldRejectCrossTenantScopedRegionDetailAccess() throws Exception {
        Region region = new Region();
        region.setId(1L);
        region.setTenantId(2L);
        region.setDeleted(0);

        when(regionMapper.selectById(1L)).thenReturn(region);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> invokeScopedGetById(99L, 1L));
        assertEquals("区域不存在或无权访问", exception.getMessage());
    }

    private Object invokeScopedPageRegions(Long currentUserId,
                                           String regionName,
                                           String regionCode,
                                           String regionType,
                                           Long pageNum,
                                           Long pageSize) throws Exception {
        try {
            Method method = RegionServiceImpl.class.getMethod(
                    "pageRegions",
                    Long.class,
                    String.class,
                    String.class,
                    String.class,
                    Long.class,
                    Long.class
            );
            return method.invoke(regionService, currentUserId, regionName, regionCode, regionType, pageNum, pageSize);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageRegions overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Region invokeScopedGetById(Long currentUserId, Long regionId) throws Exception {
        try {
            Method method = RegionServiceImpl.class.getMethod("getById", Long.class, Long.class);
            return (Region) method.invoke(regionService, currentUserId, regionId);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped getById overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Exception unwrap(InvocationTargetException exception) throws Exception {
        if (exception.getTargetException() instanceof Exception target) {
            return target;
        }
        throw exception;
    }
}
