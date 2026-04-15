package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.vo.GovernancePermissionMatrixItemVO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GovernancePermissionMatrixServiceImplTest {

    @Test
    void listMatrixShouldExposeSecretCustodyContractVendorMappingAndProtocolDualControlRows() {
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
        assertTrue(items.stream().anyMatch(item ->
                "VENDOR_MAPPING_RULE_PUBLISH".equals(item.getActionCode())
                        && "iot:product-contract:govern".equals(item.getOperatorPermissionCode())
                        && "iot:product-contract:approve".equals(item.getApproverPermissionCode())));
        assertTrue(items.stream().anyMatch(item ->
                "VENDOR_MAPPING_RULE_ROLLBACK".equals(item.getActionCode())
                        && "iot:product-contract:rollback".equals(item.getOperatorPermissionCode())
                        && "iot:product-contract:approve".equals(item.getApproverPermissionCode())));
        assertTrue(items.stream().anyMatch(item ->
                "PROTOCOL_FAMILY_PUBLISH".equals(item.getActionCode())
                        && "iot:protocol-governance:edit".equals(item.getOperatorPermissionCode())
                        && "iot:protocol-governance:approve".equals(item.getApproverPermissionCode())));
        assertTrue(items.stream().anyMatch(item ->
                "PROTOCOL_DECRYPT_PROFILE_ROLLBACK".equals(item.getActionCode())
                        && "iot:protocol-governance:edit".equals(item.getOperatorPermissionCode())
                        && "iot:protocol-governance:approve".equals(item.getApproverPermissionCode())));
        assertEquals(14, items.size());
    }
}
