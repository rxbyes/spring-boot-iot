package com.ghlzm.iot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.system.entity.InAppMessageBridgeAttemptLog;
import com.ghlzm.iot.system.vo.InAppMessageBridgeFailureCountVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface InAppMessageBridgeAttemptLogMapper extends BaseMapper<InAppMessageBridgeAttemptLog> {

    @Select("""
            <script>
            SELECT
                id,
                bridge_log_id AS bridgeLogId,
                message_id AS messageId,
                channel_code AS channelCode,
                bridge_scene AS bridgeScene,
                attempt_no AS attemptNo,
                bridge_status AS bridgeStatus,
                unread_count AS unreadCount,
                recipient_snapshot AS recipientSnapshot,
                response_status_code AS responseStatusCode,
                response_body AS responseBody,
                DATE_FORMAT(attempt_time, '%Y-%m-%d %H:%i:%s') AS attemptTime
            FROM sys_in_app_message_bridge_attempt_log
            WHERE bridge_log_id = #{bridgeLogId}
            ORDER BY attempt_no DESC, id DESC
            </script>
            """)
    List<InAppMessageBridgeAttemptVO> listAttemptsByBridgeLogId(@Param("bridgeLogId") Long bridgeLogId);

    @Select("""
            <script>
            SELECT
                attempt.channel_code AS channelCode,
                COALESCE(channel.channel_name, attempt.channel_code) AS channelName,
                COUNT(1) AS failureCount
            FROM sys_in_app_message_bridge_attempt_log attempt
            LEFT JOIN sys_notification_channel channel
                ON channel.channel_code = attempt.channel_code
                AND (channel.deleted = 0 OR channel.deleted IS NULL)
            WHERE attempt.bridge_scene = 'in_app_unread_bridge'
              AND attempt.bridge_status = 0
              AND attempt.channel_code IS NOT NULL
              AND TRIM(attempt.channel_code) <![CDATA[<>]]> ''
            <if test="startTime != null">
                AND attempt.attempt_time <![CDATA[>=]]> #{startTime}
            </if>
            GROUP BY attempt.channel_code, channel.channel_name
            ORDER BY failureCount DESC, attempt.channel_code ASC
            </script>
            """)
    List<InAppMessageBridgeFailureCountVO> listFailedAttemptCountsByChannel(@Param("startTime") Date startTime);
}
