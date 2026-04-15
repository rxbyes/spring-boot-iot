package com.ghlzm.iot.system.service;

import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;

public interface ProtocolGovernanceApprovalService {

    GovernanceSubmissionResultVO submitFamilyPublish(Long familyId, Long operatorUserId, String submitReason);

    GovernanceSubmissionResultVO submitFamilyRollback(Long familyId, Long operatorUserId, String submitReason);

    GovernanceSubmissionResultVO submitDecryptProfilePublish(Long profileId, Long operatorUserId, String submitReason);

    GovernanceSubmissionResultVO submitDecryptProfileRollback(Long profileId, Long operatorUserId, String submitReason);
}
