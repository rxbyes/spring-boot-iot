package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceWorkItemTransitionDTO;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceWorkItemControllerTest {

    @Mock
    private GovernanceWorkItemService governanceWorkItemService;

    private GovernanceWorkItemController controller;

    @BeforeEach
    void setUp() {
        controller = new GovernanceWorkItemController(governanceWorkItemService);
    }

    @Test
    void pageWorkItemsShouldDelegateToService() {
        GovernanceWorkItemPageQuery query = new GovernanceWorkItemPageQuery();
        query.setWorkStatus("OPEN");
        query.setWorkItemCode("PENDING_CONTRACT_RELEASE");
        query.setPageNum(1L);
        query.setPageSize(20L);

        GovernanceWorkItemVO row = new GovernanceWorkItemVO();
        row.setId(1L);
        row.setWorkItemCode("PENDING_CONTRACT_RELEASE");
        row.setWorkStatus("OPEN");
        when(governanceWorkItemService.pageWorkItems(query, 10001L))
                .thenReturn(PageResult.of(1L, 1L, 20L, List.of(row)));

        R<PageResult<GovernanceWorkItemVO>> response = controller.pageWorkItems(query, authentication(10001L));

        assertEquals(1L, response.getData().getTotal());
        assertEquals("PENDING_CONTRACT_RELEASE", response.getData().getRecords().get(0).getWorkItemCode());
        verify(governanceWorkItemService).pageWorkItems(query, 10001L);
    }

    @Test
    void blockWorkItemShouldDelegateToService() {
        GovernanceWorkItemTransitionDTO dto = new GovernanceWorkItemTransitionDTO();
        dto.setComment("等待外部确认");

        R<Void> response = controller.blockWorkItem(99L, dto, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(governanceWorkItemService).block(99L, 10001L, "等待外部确认");
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
