package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Dict;
import com.ghlzm.iot.system.entity.DictItem;
import com.ghlzm.iot.system.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemDictValueSupport {

    private final DictService dictService;

    public SystemDictValueSupport(DictService dictService) {
        this.dictService = dictService;
    }

    public String normalizeRequiredLowerCase(Long currentUserId,
                                             String dictCode,
                                             String rawValue,
                                             String fieldName,
                                             Set<String> fallbackValues) {
        String normalized = StringUtils.hasText(rawValue) ? rawValue.trim().toLowerCase(Locale.ROOT) : null;
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(fieldName + "不能为空");
        }
        if (!resolveAllowedValues(currentUserId, dictCode, fallbackValues).contains(normalized)) {
            throw new BizException(fieldName + "不合法");
        }
        return normalized;
    }

    private Set<String> resolveAllowedValues(Long currentUserId,
                                             String dictCode,
                                             Set<String> fallbackValues) {
        Dict dict = dictService.getByCode(currentUserId, dictCode);
        if (dict == null || dict.getItems() == null || dict.getItems().isEmpty()) {
            return normalizeFallbackValues(fallbackValues);
        }
        Set<String> dictValues = dict.getItems().stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .filter(item -> !Integer.valueOf(0).equals(item.getStatus()))
                .map(DictItem::getItemValue)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return dictValues.isEmpty() ? normalizeFallbackValues(fallbackValues) : dictValues;
    }

    private Set<String> normalizeFallbackValues(Set<String> fallbackValues) {
        if (fallbackValues == null || fallbackValues.isEmpty()) {
            return Set.of();
        }
        return fallbackValues.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
