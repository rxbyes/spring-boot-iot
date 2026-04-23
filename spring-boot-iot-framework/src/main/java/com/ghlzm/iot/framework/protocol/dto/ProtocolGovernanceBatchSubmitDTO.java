package com.ghlzm.iot.framework.protocol.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolGovernanceBatchSubmitDTO {

    @NotEmpty(message = "recordIds 不能为空")
    private List<Long> recordIds;

    private String submitReason;
}
