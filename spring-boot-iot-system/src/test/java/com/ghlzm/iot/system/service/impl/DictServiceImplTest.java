package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.mapper.DictItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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

    private DictItem dictItem(String value) {
        DictItem item = new DictItem();
        item.setItemValue(value);
        item.setStatus(1);
        item.setDeleted(0);
        return item;
    }
}
