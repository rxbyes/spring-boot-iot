package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.vo.GovernancePermissionMatrixItemVO;
import java.util.List;

/**
 * 治理权限矩阵读侧服务。
 */
public interface GovernancePermissionMatrixService {

    List<GovernancePermissionMatrixItemVO> listMatrix();
}
