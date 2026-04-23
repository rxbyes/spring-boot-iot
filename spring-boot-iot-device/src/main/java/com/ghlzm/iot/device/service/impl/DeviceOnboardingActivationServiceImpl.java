package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingBatchActivateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.service.DeviceOnboardingActivationService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DeviceOnboardingActivationServiceImpl implements DeviceOnboardingActivationService {

    private static final String STATUS_READY = "READY";
    private static final String DEFAULT_DEVICE_NAME = "未登记设备";

    private final DeviceService deviceService;

    public DeviceOnboardingActivationServiceImpl(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public DeviceOnboardingBatchResultVO activate(DeviceOnboardingBatchActivateDTO dto) {
        return activate(null, dto);
    }

    @Override
    public DeviceOnboardingBatchResultVO activate(Long currentUserId, DeviceOnboardingBatchActivateDTO dto) {
        if (dto == null || !Boolean.TRUE.equals(dto.getConfirmed())) {
            throw new BizException("请先确认接入建议");
        }

        List<String> traceIds = normalizeTraceIds(dto.getTraceIds());
        if (traceIds.isEmpty()) {
            throw new BizException("请至少选择一条接入线索");
        }

        List<String> activatedTraceIds = new ArrayList<>();
        List<String> activatedDeviceCodes = new ArrayList<>();
        List<DeviceOnboardingBatchResultVO.ErrorItem> errors = new ArrayList<>();

        for (String traceId : traceIds) {
            DeviceOnboardingSuggestionVO suggestion = null;
            try {
                suggestion = deviceService.getOnboardingSuggestion(currentUserId, new DeviceOnboardingSuggestionQuery(traceId));
                ensureSuggestionReady(suggestion);
                DeviceAddDTO payload = buildAddPayload(suggestion);
                DeviceDetailVO created = deviceService.addDevice(currentUserId, payload);
                activatedTraceIds.add(traceId);
                activatedDeviceCodes.add(resolveCreatedDeviceCode(created, payload));
            } catch (Exception ex) {
                DeviceOnboardingBatchResultVO.ErrorItem errorItem = new DeviceOnboardingBatchResultVO.ErrorItem();
                errorItem.setTraceId(traceId);
                errorItem.setDeviceCode(suggestion == null ? null : normalizeText(suggestion.getDeviceCode()));
                errorItem.setMessage(resolveErrorMessage(ex, suggestion));
                errors.add(errorItem);
            }
        }

        DeviceOnboardingBatchResultVO result = new DeviceOnboardingBatchResultVO();
        result.setRequestedCount(traceIds.size());
        result.setActivatedCount(activatedTraceIds.size());
        result.setRejectedCount(errors.size());
        result.setActivatedTraceIds(activatedTraceIds);
        result.setActivatedDeviceCodes(activatedDeviceCodes);
        result.setErrors(errors);
        return result;
    }

    private List<String> normalizeTraceIds(List<String> traceIds) {
        if (CollectionUtils.isEmpty(traceIds)) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String traceId : traceIds) {
            String value = normalizeText(traceId);
            if (StringUtils.hasText(value)) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private void ensureSuggestionReady(DeviceOnboardingSuggestionVO suggestion) {
        if (suggestion == null) {
            throw new BizException("当前线索未生成接入建议");
        }
        if (!STATUS_READY.equalsIgnoreCase(normalizeText(suggestion.getSuggestionStatus()))) {
            throw new BizException(resolveSuggestionGapMessage(suggestion));
        }
        if (CollectionUtils.isEmpty(suggestion.getRuleGaps())) {
            return;
        }
        throw new BizException(resolveSuggestionGapMessage(suggestion));
    }

    private DeviceAddDTO buildAddPayload(DeviceOnboardingSuggestionVO suggestion) {
        String recommendedProductKey = normalizeText(suggestion.getRecommendedProductKey());
        String deviceCode = normalizeText(suggestion.getDeviceCode());
        if (!StringUtils.hasText(recommendedProductKey)) {
            throw new BizException("接入建议缺少推荐产品，暂时无法转正式设备");
        }
        if (!StringUtils.hasText(deviceCode)) {
            throw new BizException("接入建议缺少设备编码，暂时无法转正式设备");
        }

        DeviceAddDTO payload = new DeviceAddDTO();
        payload.setProductKey(recommendedProductKey);
        payload.setDeviceCode(deviceCode);
        payload.setDeviceName(resolveDeviceName(suggestion));
        payload.setActivateStatus(1);
        payload.setDeviceStatus(1);
        return payload;
    }

    private String resolveDeviceName(DeviceOnboardingSuggestionVO suggestion) {
        String deviceName = normalizeText(suggestion.getDeviceName());
        if (!StringUtils.hasText(deviceName) || DEFAULT_DEVICE_NAME.equals(deviceName)) {
            return normalizeText(suggestion.getDeviceCode());
        }
        return deviceName;
    }

    private String resolveCreatedDeviceCode(DeviceDetailVO created, DeviceAddDTO payload) {
        if (created != null && StringUtils.hasText(created.getDeviceCode())) {
            return created.getDeviceCode().trim();
        }
        return payload.getDeviceCode();
    }

    private String resolveErrorMessage(Exception ex, DeviceOnboardingSuggestionVO suggestion) {
        if (ex instanceof BizException bizException && StringUtils.hasText(bizException.getMessage())) {
            return bizException.getMessage();
        }
        if (suggestion != null && !CollectionUtils.isEmpty(suggestion.getRuleGaps())) {
            return resolveSuggestionGapMessage(suggestion);
        }
        return ex.getMessage();
    }

    private String resolveSuggestionGapMessage(DeviceOnboardingSuggestionVO suggestion) {
        if (suggestion == null || CollectionUtils.isEmpty(suggestion.getRuleGaps())) {
            return "当前接入建议尚未达到可转正状态";
        }
        return suggestion.getRuleGaps().stream()
                .filter(Objects::nonNull)
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse("当前接入建议尚未达到可转正状态");
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
