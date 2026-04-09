package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.LinkageRule;

public interface RiskMetricActionBindingSyncService {

    void rebuildLinkageBindingsForRule(LinkageRule rule, Long operatorUserId, String bindingOrigin);

    void rebuildEmergencyPlanBindingsForPlan(EmergencyPlan plan, Long operatorUserId, String bindingOrigin);

    void deactivateLinkageBindings(Long linkageRuleId, Long operatorUserId);

    void deactivateEmergencyPlanBindings(Long emergencyPlanId, Long operatorUserId);
}
