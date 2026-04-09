package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.dto.RiskPointPendingBindingQuery;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingBindingItemVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 风险点待治理绑定读取服务实现。
 */
@Service
public class RiskPointPendingBindingServiceImpl implements RiskPointPendingBindingService {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final RiskPointDevicePendingBindingMapper pendingBindingMapper;
    private final RiskPointService riskPointService;

    public RiskPointPendingBindingServiceImpl(RiskPointDevicePendingBindingMapper pendingBindingMapper,
                                              RiskPointService riskPointService) {
        this.pendingBindingMapper = pendingBindingMapper;
        this.riskPointService = riskPointService;
    }

    @Override
    public PageResult<RiskPointPendingBindingItemVO> pagePendingBindings(RiskPointPendingBindingQuery query, Long currentUserId) {
        if (query == null || query.getRiskPointId() == null) {
            throw new BizException("请选择风险点");
        }
        riskPointService.getById(query.getRiskPointId(), currentUserId);
        String deviceCode = normalizeOptionalFilter(query.getDeviceCode());
        String resolutionStatus = normalizeOptionalFilter(query.getResolutionStatus());
        String batchNo = normalizeOptionalFilter(query.getBatchNo());
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        Page<RiskPointDevicePendingBinding> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RiskPointDevicePendingBinding> wrapper = new LambdaQueryWrapper<RiskPointDevicePendingBinding>()
                .eq(RiskPointDevicePendingBinding::getDeleted, 0)
                .eq(RiskPointDevicePendingBinding::getRiskPointId, query.getRiskPointId())
                .like(StringUtils.hasText(deviceCode), RiskPointDevicePendingBinding::getDeviceCode, deviceCode)
                .eq(StringUtils.hasText(resolutionStatus), RiskPointDevicePendingBinding::getResolutionStatus, resolutionStatus)
                .eq(StringUtils.hasText(batchNo), RiskPointDevicePendingBinding::getBatchNo, batchNo)
                .orderByDesc(RiskPointDevicePendingBinding::getCreateTime)
                .orderByAsc(RiskPointDevicePendingBinding::getSourceRowNo);
        Page<RiskPointDevicePendingBinding> result = pendingBindingMapper.selectPage(page, wrapper);
        List<RiskPointPendingBindingItemVO> records = result.getRecords().stream()
                .map(this::toItem)
                .toList();
        return PageResult.of(result.getTotal(), pageNum, pageSize, records);
    }

    @Override
    public RiskPointDevicePendingBinding getRequiredPending(Long pendingId, Long currentUserId) {
        if (pendingId == null) {
            throw new BizException("待治理记录不存在");
        }
        RiskPointDevicePendingBinding pending = pendingBindingMapper.selectOne(new LambdaQueryWrapper<RiskPointDevicePendingBinding>()
                .eq(RiskPointDevicePendingBinding::getId, pendingId)
                .eq(RiskPointDevicePendingBinding::getDeleted, 0));
        if (pending == null) {
            throw new BizException("待治理记录不存在");
        }
        if (pending.getRiskPointId() != null) {
            riskPointService.getById(pending.getRiskPointId(), currentUserId);
        }
        return pending;
    }

    private RiskPointPendingBindingItemVO toItem(RiskPointDevicePendingBinding pending) {
        RiskPointPendingBindingItemVO item = new RiskPointPendingBindingItemVO();
        item.setId(pending.getId());
        item.setBatchNo(pending.getBatchNo());
        item.setSourceFileName(pending.getSourceFileName());
        item.setSourceRowNo(pending.getSourceRowNo());
        item.setRiskPointId(pending.getRiskPointId());
        item.setRiskPointCode(pending.getRiskPointCode());
        item.setRiskPointName(pending.getRiskPointName());
        item.setDeviceId(pending.getDeviceId());
        item.setDeviceCode(pending.getDeviceCode());
        item.setDeviceName(pending.getDeviceName());
        item.setResolutionStatus(pending.getResolutionStatus());
        item.setResolutionNote(pending.getResolutionNote());
        item.setMetricIdentifier(pending.getMetricIdentifier());
        item.setMetricName(pending.getMetricName());
        item.setPromotedBindingId(pending.getPromotedBindingId());
        item.setPromotedTime(pending.getPromotedTime());
        item.setCreateTime(pending.getCreateTime());
        return item;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private String normalizeOptionalFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
