package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.alarm.dto.RiskMonitoringListQuery;
import com.ghlzm.iot.alarm.vo.RiskMonitoringDetailVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringGisPointVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringListItemVO;
import com.ghlzm.iot.common.response.PageResult;

import java.util.List;

/**
 * 风险监测服务。
 */
public interface RiskMonitoringService {

    PageResult<RiskMonitoringListItemVO> listRealtimeItems(RiskMonitoringListQuery query);

    RiskMonitoringDetailVO getRealtimeDetail(Long bindingId);

    List<RiskMonitoringGisPointVO> listGisPoints(Long regionId);
}
