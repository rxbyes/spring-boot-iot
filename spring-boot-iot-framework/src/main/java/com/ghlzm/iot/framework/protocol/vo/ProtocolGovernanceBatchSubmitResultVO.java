package com.ghlzm.iot.framework.protocol.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProtocolGovernanceBatchSubmitResultVO {

    private Integer totalCount;
    private Integer submittedCount;
    private Integer failedCount;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {

        private Long recordId;
        private Boolean success;
        private Long approvalOrderId;
        private String errorMessage;
    }
}
