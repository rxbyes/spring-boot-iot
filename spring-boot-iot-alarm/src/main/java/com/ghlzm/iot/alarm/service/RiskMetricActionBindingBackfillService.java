package com.ghlzm.iot.alarm.service;

public interface RiskMetricActionBindingBackfillService {

    void rebuildAllLinkageBindings();

    void rebuildAllEmergencyPlanBindings();

    void ensureBindingsReadyForRead();
}
