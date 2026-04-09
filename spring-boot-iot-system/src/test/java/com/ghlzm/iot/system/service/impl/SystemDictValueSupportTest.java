package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.DictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemDictValueSupportTest {

    @Mock
    private DictService dictService;

    private SystemDictValueSupport systemDictValueSupport;

    @BeforeEach
    void setUp() {
        systemDictValueSupport = new SystemDictValueSupport(dictService);
    }

    @Test
    void shouldFallbackToDefaultValuesWhenDictMissing() {
        when(dictService.getByCode(99L, "help_doc_category")).thenReturn(null);

        String normalized = systemDictValueSupport.normalizeRequiredLowerCase(
                99L,
                "help_doc_category",
                "BUSINESS",
                "文档分类",
                Set.of("business", "technical", "faq")
        );

        assertEquals("business", normalized);
    }

    @Test
    void shouldRejectValueOutsideDictAndFallback() {
        when(dictService.getByCode(99L, "help_doc_category")).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () ->
                systemDictValueSupport.normalizeRequiredLowerCase(
                        99L,
                        "help_doc_category",
                        "whitepaper",
                        "文档分类",
                        Set.of("business", "technical", "faq")
                ));

        assertEquals("文档分类不合法", exception.getMessage());
    }
}
