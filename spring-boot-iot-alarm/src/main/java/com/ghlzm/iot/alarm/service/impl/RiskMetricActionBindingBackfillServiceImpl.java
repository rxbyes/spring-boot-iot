package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RiskMetricActionBindingBackfillServiceImpl implements RiskMetricActionBindingBackfillService {

    private final LinkageRuleMapper linkageRuleMapper;
    private final EmergencyPlanMapper emergencyPlanMapper;
    private final RiskMetricLinkageBindingMapper linkageBindingMapper;
    private final RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;
    private final RiskMetricActionBindingSyncService bindingSyncService;
    private final AtomicBoolean linkageBackfillAttempted = new AtomicBoolean(false);
    private final AtomicBoolean emergencyPlanBackfillAttempted = new AtomicBoolean(false);

    public RiskMetricActionBindingBackfillServiceImpl(LinkageRuleMapper linkageRuleMapper,
                                                      EmergencyPlanMapper emergencyPlanMapper,
                                                      RiskMetricLinkageBindingMapper linkageBindingMapper,
                                                      RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper,
                                                      RiskMetricActionBindingSyncService bindingSyncService) {
        this.linkageRuleMapper = linkageRuleMapper;
        this.emergencyPlanMapper = emergencyPlanMapper;
        this.linkageBindingMapper = linkageBindingMapper;
        this.emergencyPlanBindingMapper = emergencyPlanBindingMapper;
        this.bindingSyncService = bindingSyncService;
    }

    @Override
    public void rebuildAllLinkageBindings() {
        linkageRuleMapper.selectList(new LambdaQueryWrapper<LinkageRule>()
                        .eq(LinkageRule::getDeleted, 0)
                        .eq(LinkageRule::getStatus, 0))
                .forEach(rule -> bindingSyncService.rebuildLinkageBindingsForRule(rule, 0L, "BACKFILL"));
    }

    @Override
    public void rebuildAllEmergencyPlanBindings() {
        emergencyPlanMapper.selectList(new LambdaQueryWrapper<EmergencyPlan>()
                        .eq(EmergencyPlan::getDeleted, 0)
                        .eq(EmergencyPlan::getStatus, 0))
                .forEach(plan -> bindingSyncService.rebuildEmergencyPlanBindingsForPlan(plan, 0L, "BACKFILL"));
    }

    @Override
    public void ensureBindingsReadyForRead() {
        long activeLinkageRuleCount = linkageRuleMapper.selectCount(new LambdaQueryWrapper<LinkageRule>()
                .eq(LinkageRule::getDeleted, 0)
                .eq(LinkageRule::getStatus, 0));
        long activeEmergencyPlanCount = emergencyPlanMapper.selectCount(new LambdaQueryWrapper<EmergencyPlan>()
                .eq(EmergencyPlan::getDeleted, 0)
                .eq(EmergencyPlan::getStatus, 0));

        if (shouldBackfillLinkageBindings(activeLinkageRuleCount)) {
            rebuildAllLinkageBindings();
        }
        if (shouldBackfillEmergencyPlanBindings(activeEmergencyPlanCount)) {
            rebuildAllEmergencyPlanBindings();
        }
    }

    private boolean shouldBackfillLinkageBindings(long activeLinkageRuleCount) {
        if (activeLinkageRuleCount <= 0L || !linkageBackfillAttempted.compareAndSet(false, true)) {
            return false;
        }
        long activeLinkageBindingCount = linkageBindingMapper.selectCount(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                .eq(RiskMetricLinkageBinding::getDeleted, 0)
                .eq(RiskMetricLinkageBinding::getBindingStatus, "ACTIVE"));
        if (activeLinkageBindingCount == 0L) {
            return true;
        }
        Set<Long> coveredRuleIds = linkageBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                        .eq(RiskMetricLinkageBinding::getDeleted, 0)
                        .eq(RiskMetricLinkageBinding::getBindingStatus, "ACTIVE"))
                .stream()
                .map(RiskMetricLinkageBinding::getLinkageRuleId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return coveredRuleIds.size() < activeLinkageRuleCount;
    }

    private boolean shouldBackfillEmergencyPlanBindings(long activeEmergencyPlanCount) {
        if (activeEmergencyPlanCount <= 0L || !emergencyPlanBackfillAttempted.compareAndSet(false, true)) {
            return false;
        }
        long activePlanBindingCount = emergencyPlanBindingMapper.selectCount(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, "ACTIVE"));
        if (activePlanBindingCount == 0L) {
            return true;
        }
        Set<Long> coveredPlanIds = emergencyPlanBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                        .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                        .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, "ACTIVE"))
                .stream()
                .map(RiskMetricEmergencyPlanBinding::getEmergencyPlanId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return coveredPlanIds.size() < activeEmergencyPlanCount;
    }
}
