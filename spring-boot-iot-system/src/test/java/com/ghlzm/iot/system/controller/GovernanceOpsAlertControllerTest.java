package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceOpsAlertTransitionDTO;
import com.ghlzm.iot.system.service.GovernanceOpsAlertService;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertPageQuery;
import com.ghlzm.iot.system.vo.GovernanceOpsAlertVO;
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
class GovernanceOpsAlertControllerTest {

    @Mock
    private GovernanceOpsAlertService governanceOpsAlertService;

    private GovernanceOpsAlertController controller;

    @BeforeEach
    void setUp() {
        controller = new GovernanceOpsAlertController(governanceOpsAlertService);
    }

    @Test
    void pageAlertsShouldDelegateToService() {
        GovernanceOpsAlertPageQuery query = new GovernanceOpsAlertPageQuery();
        query.setAlertStatus("OPEN");
        query.setAlertType("FIELD_DRIFT");
        query.setPageNum(1L);
        query.setPageSize(20L);

        GovernanceOpsAlertVO row = new GovernanceOpsAlertVO();
        row.setId(1L);
        row.setAlertType("FIELD_DRIFT");
        row.setAlertStatus("OPEN");
        when(governanceOpsAlertService.pageAlerts(query, 10001L))
                .thenReturn(PageResult.of(1L, 1L, 20L, List.of(row)));

        R<PageResult<GovernanceOpsAlertVO>> response = controller.pageAlerts(query, authentication(10001L));

        assertEquals(1L, response.getData().getTotal());
        assertEquals("FIELD_DRIFT", response.getData().getRecords().get(0).getAlertType());
        verify(governanceOpsAlertService).pageAlerts(query, 10001L);
    }

    @Test
    void suppressAlertShouldDelegateToService() {
        GovernanceOpsAlertTransitionDTO dto = new GovernanceOpsAlertTransitionDTO();
        dto.setComment("临时抑制");

        R<Void> response = controller.suppressAlert(99L, dto, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(governanceOpsAlertService).suppress(99L, 10001L, "临时抑制");
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
