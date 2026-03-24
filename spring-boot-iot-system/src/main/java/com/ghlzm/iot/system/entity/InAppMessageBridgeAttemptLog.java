package com.ghlzm.iot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 站内消息桥接尝试明细。
 */
@Data
@TableName("sys_in_app_message_bridge_attempt_log")
public class InAppMessageBridgeAttemptLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long bridgeLogId;

    private Long messageId;

    private String channelCode;

    private String bridgeScene;

    private Integer attemptNo;

    /**
     * 桥接状态：0 失败，1 成功。
     */
    private Integer bridgeStatus;

    private Integer unreadCount;

    private String recipientSnapshot;

    private Integer responseStatusCode;

    private String responseBody;

    private Date attemptTime;

    private Date createTime;
}
