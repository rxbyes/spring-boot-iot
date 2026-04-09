package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.vo.GovernancePermissionMatrixItemVO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GovernancePermissionMatrixServiceImplTest {

    @Test
    void listMatrixShouldExposeSecretCustodyAndContractDualControlRows() {
        GovernancePermissionMatrixServiceImpl service = new GovernancePermissionMatrixServiceImpl();

        List<GovernancePermissionMatrixItemVO> items = service.listMatrix();

        assertTrue(items.stream().anyMatch(item ->
                "iot:secret-custody:rotate".equals(item.getOperatorPermissionCode())
                        && "iot:secret-custody:approve".equals(item.getApproverPermissionCode())
                        && Boolean.TRUE.equals(item.getDualControlRequired())));
        assertTrue(items.stream().anyMatch(item ->
                "iot:product-contract:release".equals(item.getOperatorPermissionCode())
                        && "iot:product-contract:approve".equals(item.getApproverPermissionCode())
                        && item.getDefaultRoleCodes().contains("OPS_STAFF")
                        && item.getDefaultApproverRoleCodes().contains("MANAGEMENT_STAFF")));
        assertEquals(8, items.size());
    }
}
