package com.ghlzm.iot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.system.entity.InAppMessageBridgeAttemptLog;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
