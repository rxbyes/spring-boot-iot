package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskMonitoringListQuery;
import com.ghlzm.iot.alarm.service.RiskMonitoringService;
import com.ghlzm.iot.alarm.vo.RiskMonitoringDetailVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringGisPointVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringListItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 风险监测只读接口。
 */
@RestController
@RequestMapping("/api/risk-monitoring")
public class RiskMonitoringController {

    private final RiskMonitoringService riskMonitoringService;

    public RiskMonitoringController(RiskMonitoringService riskMonitoringService) {
        this.riskMonitoringService = riskMonitoringService;
    }

    @GetMapping("/realtime/list")
    public R<PageResult<RiskMonitoringListItemVO>> listRealtimeItems(RiskMonitoringListQuery query) {
        return R.ok(riskMonitoringService.listRealtimeItems(query));
    }

    @GetMapping("/realtime/{bindingId}")
    public R<RiskMonitoringDetailVO> getRealtimeDetail(@PathVariable("bindingId") Long bindingId) {
        return R.ok(riskMonitoringService.getRealtimeDetail(bindingId));
    }

    @GetMapping("/gis/points")
    public R<List<RiskMonitoringGisPointVO>> listGisPoints(
            @RequestParam(value = "regionId", required = false) Long regionId) {
        return R.ok(riskMonitoringService.listGisPoints(regionId));
    }
}
