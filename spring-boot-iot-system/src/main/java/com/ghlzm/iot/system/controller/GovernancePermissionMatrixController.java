package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.service.GovernancePermissionMatrixService;
import com.ghlzm.iot.system.vo.GovernancePermissionMatrixItemVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 治理权限矩阵控制器。
 */
@RestController
public class GovernancePermissionMatrixController {

    private final GovernancePermissionMatrixService governancePermissionMatrixService;

    public GovernancePermissionMatrixController(GovernancePermissionMatrixService governancePermissionMatrixService) {
        this.governancePermissionMatrixService = governancePermissionMatrixService;
    }

    @GetMapping("/api/system/governance/permission-matrix")
    public R<List<GovernancePermissionMatrixItemVO>> listMatrix() {
        return R.ok(governancePermissionMatrixService.listMatrix());
    }
}
