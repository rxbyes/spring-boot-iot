package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.DictMapper;
import com.ghlzm.iot.system.mapper.DictItemMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictServiceImplTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private DictMapper dictMapper;
    @Mock
    private DictItemMapper dictItemMapper;

    private DictServiceImpl dictService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Dict.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        dictService = spy(new DictServiceImpl(dictItemMapper, permissionService));
        Field field = findField(dictService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(dictService, dictMapper);
    }

    @Test
    void getByCodeShouldLoadEnabledItems() {
        Dict dict = new Dict();
        dict.setId(9001L);
        dict.setDictCode("risk_level");

        DictItem red = dictItem("red");
        DictItem orange = dictItem("orange");

        doReturn(dict).when(dictService).getOne(any());
        when(dictItemMapper.selectList(any())).thenReturn(List.of(red, orange));

        Dict result = dictService.getByCode("risk_level");

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals("red", result.getItems().get(0).getItemValue());
        assertEquals("orange", result.getItems().get(1).getItemValue());
        verify(dictItemMapper).selectList(any());
    }

    @Test
    void getByCodeShouldSkipItemLookupWhenDictMissing() {
        doReturn(null).when(dictService).getOne(any());

        Dict result = dictService.getByCode("risk_level");

        assertNull(result);
        verify(dictItemMapper, never()).selectList(any());
    }

    @Test
    void addDictItemShouldApplyParentTenantAndDefaults() throws Exception {
        Dict parent = new Dict();
        parent.setId(9001L);
        parent.setTenantId(1L);

        DictItem input = new DictItem();
        input.setDictId(9001L);
        input.setItemName("红色");
        input.setItemValue("red");

        doReturn(parent).when(dictService).getById(9001L);
        when(dictItemMapper.selectCount(any())).thenReturn(0L);
        when(dictItemMapper.insert(org.mockito.ArgumentMatchers.<DictItem>any())).thenAnswer(invocation -> {
            DictItem inserted = invocation.getArgument(0);
            inserted.setId(7001L);
            return 1;
        });

        DictItem result = invokeDictItemMutation("addDictItem", input);

        assertEquals(7001L, result.getId());
        assertEquals(1L, result.getTenantId());
        assertEquals(1, result.getStatus());
        assertEquals(0, result.getSortNo());
        verify(dictItemMapper).insert(same(result));
    }

    @Test
    void addDictItemShouldRejectDuplicateValueWithinSameDict() throws Exception {
        Dict parent = new Dict();
        parent.setId(9001L);
        parent.setTenantId(1L);

        DictItem input = new DictItem();
        input.setDictId(9001L);
        input.setItemName("红色");
        input.setItemValue("red");

        doReturn(parent).when(dictService).getById(9001L);
        when(dictItemMapper.selectCount(any())).thenReturn(1L);

        InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> invokeDictItemMutation("addDictItem", input));

        assertEquals("字典项值已存在", error.getCause().getMessage());
        verify(dictItemMapper, never()).insert(org.mockito.ArgumentMatchers.<DictItem>any());
    }

    @Test
    void updateDictItemShouldMergeExistingItem() throws Exception {
        Dict parent = new Dict();
        parent.setId(9001L);
        parent.setTenantId(1L);

        DictItem existing = new DictItem();
        existing.setId(7001L);
        existing.setTenantId(1L);
        existing.setDictId(9001L);
        existing.setItemName("旧名称");
        existing.setItemValue("old");
        existing.setItemType("string");
        existing.setStatus(1);
        existing.setSortNo(3);
        existing.setRemark("旧备注");
        existing.setDeleted(0);

        DictItem input = new DictItem();
        input.setId(7001L);
        input.setDictId(9001L);
        input.setItemName("新名称");
        input.setItemValue("new");
        input.setItemType("number");
        input.setStatus(0);
        input.setSortNo(9);
        input.setRemark("新备注");

        doReturn(parent).when(dictService).getById(9001L);
        when(dictItemMapper.selectById(7001L)).thenReturn(existing);
        when(dictItemMapper.selectCount(any())).thenReturn(0L);
        when(dictItemMapper.updateById(org.mockito.ArgumentMatchers.<DictItem>any())).thenReturn(1);

        invokeDictItemMutation("updateDictItem", input);

        assertEquals("新名称", existing.getItemName());
        assertEquals("new", existing.getItemValue());
        assertEquals("number", existing.getItemType());
        assertEquals(0, existing.getStatus());
        assertEquals(9, existing.getSortNo());
        assertEquals("新备注", existing.getRemark());
        verify(dictItemMapper).updateById(same(existing));
    }

    @Test
    void deleteDictItemShouldDeleteById() throws Exception {
        Method method = DictServiceImpl.class.getMethod("deleteDictItem", Long.class);

        method.invoke(dictService, 7001L);

        verify(dictItemMapper).deleteById(7001L);
    }

    @Test
    void shouldFilterScopedDictPageToCurrentTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        Page<Dict> page = new Page<>(1L, 10L);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(dictService).page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class));

        invokeScopedPageDicts(99L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Dict>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(dictService).page(org.mockito.ArgumentMatchers.any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldRejectCrossTenantScopedDictDetailAccess() throws Exception {
        Dict dict = new Dict();
        dict.setId(1L);
        dict.setTenantId(2L);
        dict.setDeleted(0);

        when(dictMapper.selectById(1L)).thenReturn(dict);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> invokeScopedGetById(99L, 1L));
        assertEquals("字典不存在或无权访问", exception.getMessage());
    }

    private DictItem dictItem(String value) {
        DictItem item = new DictItem();
        item.setItemValue(value);
        item.setStatus(1);
        item.setDeleted(0);
        return item;
    }

    private DictItem invokeDictItemMutation(String methodName, DictItem dictItem) throws Exception {
        Method method = DictServiceImpl.class.getMethod(methodName, DictItem.class);
        return (DictItem) method.invoke(dictService, dictItem);
    }

    private Object invokeScopedPageDicts(Long currentUserId,
                                         String dictName,
                                         String dictCode,
                                         String dictType,
                                         Long pageNum,
                                         Long pageSize) throws Exception {
        try {
            Method method = DictServiceImpl.class.getMethod(
                    "pageDicts",
                    Long.class,
                    String.class,
                    String.class,
                    String.class,
                    Long.class,
                    Long.class
            );
            return method.invoke(dictService, currentUserId, dictName, dictCode, dictType, pageNum, pageSize);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageDicts overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Dict invokeScopedGetById(Long currentUserId, Long dictId) throws Exception {
        try {
            Method method = DictServiceImpl.class.getMethod("getById", Long.class, Long.class);
            return (Dict) method.invoke(dictService, currentUserId, dictId);
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
