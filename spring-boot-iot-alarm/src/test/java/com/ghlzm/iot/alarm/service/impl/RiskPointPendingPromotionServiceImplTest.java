package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.dto.RiskPointPendingIgnoreRequest;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionMetricDTO;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionRequest;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingMetricCandidateVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionItemVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionResultVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointPendingPromotionServiceImplTest {

    @Test
    void promoteShouldCreateMultipleFormalBindingsAndWritePromotionHistory() {
        Fixture fixture = new Fixture();
        RiskPointDevicePendingBinding pending = fixture.pending("PENDING_METRIC_GOVERNANCE");
        pending.setDeviceName("固定测斜仪-L1");
        RiskPoint riskPoint = fixture.riskPoint();
        RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
        bundle.setDeviceName("固定测斜仪-L1");
        bundle.setCandidates(List.of(
                fixture.candidate("dispsX", "X向位移", "HIGH", 100, 7001L),
                fixture.candidate("dispsY", "Y向位移", "HIGH", 96, 7002L)
        ));

        when(fixture.pendingBindingMapper.selectByIdForUpdate(77L)).thenReturn(pending);
        when(fixture.recommendationService.getCandidates(77L, 1001L)).thenReturn(bundle);
        when(fixture.riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(riskPoint);
        when(fixture.riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), eq(1001L))).thenAnswer(invocation -> {
            RiskPointDevice binding = invocation.getArgument(0);
            binding.setId("dispsX".equals(binding.getMetricIdentifier()) ? 9001L : 9002L);
            return binding;
        });

        RiskPointPendingPromotionRequest request = new RiskPointPendingPromotionRequest();
        request.setCompletePending(true);
        request.setPromotionNote("人工确认后转正");
        request.setMetrics(List.of(
                fixture.metric("dispsX", "X向位移"),
                fixture.metric("L1_SW_1.dispsY", "Y向位移")
        ));

        RiskPointPendingPromotionResultVO result = fixture.service.promote(77L, request, 1001L);

        assertEquals("PROMOTED", result.getPendingStatus());
        assertEquals(2, result.getItems().size());
        assertEquals("SUCCESS", result.getItems().get(0).getPromotionStatus());
        assertEquals("SUCCESS", result.getItems().get(1).getPromotionStatus());
        verify(fixture.promotionMapper, times(2)).insert(any(RiskPointDevicePendingPromotion.class));
        verify(fixture.riskPointService).bindDeviceAndReturn(argThat(
                binding -> "dispsY".equals(binding.getMetricIdentifier())
                        && Long.valueOf(7002L).equals(binding.getRiskMetricId())
        ), eq(1001L));
        verify(fixture.pendingBindingMapper).updateById(org.mockito.ArgumentMatchers.<RiskPointDevicePendingBinding>argThat(row ->
                "PROMOTED".equals(row.getResolutionStatus())
                        && Long.valueOf(9002L).equals(row.getPromotedBindingId())
                        && "人工确认后转正".equals(row.getResolutionNote())
        ));
    }

    @Test
    void promoteShouldMarkDuplicateSkippedAndReuseExistingFormalBindingId() {
        Fixture fixture = new Fixture();
        RiskPointDevicePendingBinding pending = fixture.pending("PENDING_METRIC_GOVERNANCE");
        RiskPoint riskPoint = fixture.riskPoint();
        RiskPointDevice existing = new RiskPointDevice();
        existing.setId(9010L);
        existing.setRiskPointId(12L);
        existing.setDeviceId(2001L);
        existing.setMetricIdentifier("dispsX");
        RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
        bundle.setCandidates(List.of(fixture.candidate("dispsX", "X向位移", "HIGH", 100, 7001L)));

        when(fixture.pendingBindingMapper.selectByIdForUpdate(77L)).thenReturn(pending);
        when(fixture.recommendationService.getCandidates(77L, 1001L)).thenReturn(bundle);
        when(fixture.riskPointDeviceMapper.selectOne(any())).thenReturn(existing);
        when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(riskPoint);

        RiskPointPendingPromotionRequest request = new RiskPointPendingPromotionRequest();
        request.setMetrics(List.of(fixture.metric("dispsX", "X向位移")));

        RiskPointPendingPromotionResultVO result = fixture.service.promote(77L, request, 1001L);

        RiskPointPendingPromotionItemVO item = result.getItems().get(0);
        assertEquals("DUPLICATE_SKIPPED", item.getPromotionStatus());
        assertEquals(9010L, item.getBindingId());
        verify(fixture.riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(fixture.promotionMapper).insert(org.mockito.ArgumentMatchers.<RiskPointDevicePendingPromotion>argThat(
                row -> Long.valueOf(9010L).equals(row.getRiskPointDeviceId())
        ));
    }

    @Test
    void ignoreShouldMarkPendingIgnoredAndPersistNote() {
        Fixture fixture = new Fixture();
        RiskPointDevicePendingBinding pending = fixture.pending("PARTIALLY_PROMOTED");
        RiskPoint riskPoint = fixture.riskPoint();
        when(fixture.pendingBindingMapper.selectByIdForUpdate(77L)).thenReturn(pending);
        when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(riskPoint);

        RiskPointPendingIgnoreRequest request = new RiskPointPendingIgnoreRequest();
        request.setIgnoreNote("人工判定无需转正");

        fixture.service.ignore(77L, request, 1001L);

        verify(fixture.pendingBindingMapper).updateById(org.mockito.ArgumentMatchers.<RiskPointDevicePendingBinding>argThat(row ->
                "IGNORED".equals(row.getResolutionStatus())
                        && "人工判定无需转正".equals(row.getResolutionNote())
                        && Long.valueOf(1001L).equals(row.getUpdateBy())
        ));
        verify(fixture.promotionMapper, never()).insert(any(RiskPointDevicePendingPromotion.class));
    }

    @Test
    void promoteShouldRejectStatusMetricForFixedInclinometerEvenIfCandidateExists() {
        Fixture fixture = new Fixture();
        RiskPointDevicePendingBinding pending = fixture.pending("PENDING_METRIC_GOVERNANCE");
        pending.setDeviceName("固定测斜仪-L1");
        RiskPoint riskPoint = fixture.riskPoint();
        RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
        bundle.setDeviceName("固定测斜仪-L1");
        bundle.setCandidates(List.of(
                fixture.candidate("battery_dump_energy", "电池电量", "MEDIUM", 60, null),
                fixture.candidate("dispsX", "X向位移", "HIGH", 100, 7001L)
        ));

        when(fixture.pendingBindingMapper.selectByIdForUpdate(77L)).thenReturn(pending);
        when(fixture.recommendationService.getCandidates(77L, 1001L)).thenReturn(bundle);
        when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(riskPoint);

        RiskPointPendingPromotionRequest request = new RiskPointPendingPromotionRequest();
        request.setMetrics(List.of(fixture.metric("battery_dump_energy", "电池电量")));

        RiskPointPendingPromotionResultVO result = fixture.service.promote(77L, request, 1001L);

        assertEquals(1, result.getItems().size());
        assertEquals("INVALID_METRIC", result.getItems().get(0).getPromotionStatus());
        verify(fixture.riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(fixture.riskPointDeviceMapper, never()).selectOne(any());
    }

    private static final class Fixture {
        private final RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        private final RiskPointDevicePendingPromotionMapper promotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        private final RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        private final RiskPointPendingRecommendationService recommendationService = mock(RiskPointPendingRecommendationService.class);
        private final RiskPointService riskPointService = mock(RiskPointService.class);
        private final RiskPointPendingPromotionServiceImpl service = new RiskPointPendingPromotionServiceImpl(
                pendingBindingMapper,
                promotionMapper,
                riskPointDeviceMapper,
                recommendationService,
                riskPointService
        );

        private RiskPointDevicePendingBinding pending(String resolutionStatus) {
            RiskPointDevicePendingBinding value = new RiskPointDevicePendingBinding();
            value.setId(77L);
            value.setRiskPointId(12L);
            value.setDeviceId(2001L);
            value.setDeviceCode("DEVICE-2001");
            value.setDeviceName("一号设备");
            value.setResolutionStatus(resolutionStatus);
            value.setTenantId(1L);
            value.setDeleted(0);
            return value;
        }

        private RiskPoint riskPoint() {
            RiskPoint value = new RiskPoint();
            value.setId(12L);
            value.setRiskPointCode("RP-OPS-001");
            value.setRiskPointName("一号风险点");
            return value;
        }

        private RiskPointPendingMetricCandidateVO candidate(String identifier,
                                                            String name,
                                                            String level,
                                                            int score,
                                                            Long riskMetricId) {
            RiskPointPendingMetricCandidateVO value = new RiskPointPendingMetricCandidateVO();
            value.setRiskMetricId(riskMetricId);
            value.setMetricIdentifier(identifier);
            value.setMetricName(name);
            value.setRecommendationLevel(level);
            value.setRecommendationScore(score);
            value.setEvidenceSources(List.of("MESSAGE_LOG"));
            return value;
        }

        private RiskPointPendingPromotionMetricDTO metric(String identifier, String name) {
            RiskPointPendingPromotionMetricDTO value = new RiskPointPendingPromotionMetricDTO();
            value.setMetricIdentifier(identifier);
            value.setMetricName(name);
            return value;
        }
    }
}
