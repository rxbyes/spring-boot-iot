package com.ghlzm.iot.alarm.vo;

import lombok.Data;

import java.util.List;

/**
 * 待治理转正汇总结果。
 */
@Data
public class RiskPointPendingPromotionResultVO {

    private Long pendingId;

    private String pendingStatus;

    private List<RiskPointPendingPromotionItemVO> items;
}
