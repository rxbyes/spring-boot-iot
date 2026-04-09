package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.entity.GovernanceApprovalTransition;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.GovernanceApprovalOrderMapper;
import com.ghlzm.iot.system.mapper.GovernanceApprovalTransitionMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderVO;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceApprovalQueryServiceImplTest {

    @Mock
    private GovernanceApprovalOrderMapper orderMapper;

    @Mock
    private GovernanceApprovalTransitionMapper transitionMapper;

    @Mock
    private PermissionService permissionService;

    @Test
    void pageOrdersShouldReturnPagedResult() {
        GovernanceApprovalQueryServiceImpl service = new GovernanceApprovalQueryServiceImpl(
                orderMapper,
                transitionMapper,
                permissionService
        );
        when(permissionService.getDataPermissionContext(10001L))
                .thenReturn(new DataPermissionContext(10001L, 2001L, 3001L, DataScopeType.ALL, false));
        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(99001L);
        order.setActionCode("PRODUCT_CONTRACT_RELEASE_APPLY");
        order.setStatus("APPROVED");
        order.setCreateTime(new Date());
        when(orderMapper.selectPage(any(), any()))
                .thenReturn(new Page<GovernanceApprovalOrder>(1L, 10L, 1L).setRecords(List.of(order)));

        PageResult<GovernanceApprovalOrderVO> page = service.pageOrders(
                10001L,
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "PRODUCT",
                1001L,
                "approved",
                10001L,
                20002L,
                1L,
                10L
        );

        assertEquals(1L, page.getTotal());
        assertEquals(99001L, page.getRecords().get(0).getId());
        assertEquals("APPROVED", page.getRecords().get(0).getStatus());
        verify(orderMapper).selectPage(any(), any());
    }

    @Test
    void getOrderDetailShouldReturnTransitions() {
        GovernanceApprovalQueryServiceImpl service = new GovernanceApprovalQueryServiceImpl(
                orderMapper,
                transitionMapper,
                permissionService
        );
        when(permissionService.getDataPermissionContext(10001L))
                .thenReturn(new DataPermissionContext(10001L, 2001L, 3001L, DataScopeType.ALL, false));
        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(99001L);
        order.setTenantId(2001L);
        GovernanceApprovalTransition transition = new GovernanceApprovalTransition();
        transition.setId(99011L);
        transition.setToStatus("APPROVED");
        transition.setCreateTime(new Date());
        when(orderMapper.selectById(99001L)).thenReturn(order);
        when(transitionMapper.selectList(any())).thenReturn(List.of(transition));

        GovernanceApprovalOrderDetailVO detail = service.getOrderDetail(10001L, 99001L);

        assertEquals(99001L, detail.getOrder().getId());
        assertEquals(1, detail.getTransitions().size());
        assertEquals("APPROVED", detail.getTransitions().get(0).getToStatus());
    }

    @Test
    void getOrderDetailShouldRejectCrossTenantAccess() {
        GovernanceApprovalQueryServiceImpl service = new GovernanceApprovalQueryServiceImpl(
                orderMapper,
                transitionMapper,
                permissionService
        );
        when(permissionService.getDataPermissionContext(10001L))
                .thenReturn(new DataPermissionContext(10001L, 2001L, 3001L, DataScopeType.ALL, false));
        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(99001L);
        order.setTenantId(9999L);
        when(orderMapper.selectById(99001L)).thenReturn(order);

        assertThrows(BizException.class, () -> service.getOrderDetail(10001L, 99001L));
        verify(transitionMapper, never()).selectList(any());
    }
}
