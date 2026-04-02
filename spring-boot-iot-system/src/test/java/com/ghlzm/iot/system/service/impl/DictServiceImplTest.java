package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.mapper.DictItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private DictItemMapper dictItemMapper;

    private DictServiceImpl dictService;

    @BeforeEach
    void setUp() {
        dictService = spy(new DictServiceImpl(dictItemMapper));
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
}
