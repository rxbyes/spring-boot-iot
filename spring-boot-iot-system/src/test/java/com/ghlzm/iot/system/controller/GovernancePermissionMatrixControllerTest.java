package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.service.GovernancePermissionMatrixService;
import com.ghlzm.iot.system.vo.GovernancePermissionMatrixItemVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernancePermissionMatrixControllerTest {

    @Mock
    private GovernancePermissionMatrixService governancePermissionMatrixService;

    private GovernancePermissionMatrixController controller;

    @BeforeEach
    void setUp() {
        controller = new GovernancePermissionMatrixController(governancePermissionMatrixService);
    }

    @Test
    void listMatrixShouldDelegateToService() {
        GovernancePermissionMatrixItemVO item = new GovernancePermissionMatrixItemVO();
        item.setActionCode("DEVICE_SECRET_ROTATE");
        item.setOperatorPermissionCode("iot:secret-custody:rotate");
        item.setApproverPermissionCode("iot:secret-custody:approve");
        when(governancePermissionMatrixService.listMatrix()).thenReturn(List.of(item));

        R<List<GovernancePermissionMatrixItemVO>> response = controller.listMatrix();

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals("DEVICE_SECRET_ROTATE", response.getData().get(0).getActionCode());
        verify(governancePermissionMatrixService).listMatrix();
    }
}
