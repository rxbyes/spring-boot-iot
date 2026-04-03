package com.ghlzm.iot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.system.entity.InAppMessageBridgeLog;
import com.ghlzm.iot.system.vo.InAppMessageBridgeLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface InAppMessageBridgeLogMapper extends BaseMapper<InAppMessageBridgeLog> {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_in_app_message_bridge_log bridge_log
            LEFT JOIN sys_in_app_message message
                ON message.id = bridge_log.message_id
            LEFT JOIN sys_notification_channel channel
                ON channel.channel_code = bridge_log.channel_code
                AND (channel.deleted = 0 OR channel.deleted IS NULL)
            WHERE bridge_log.bridge_scene = 'in_app_unread_bridge'
            <if test="tenantId != null">
                AND bridge_log.tenant_id = #{tenantId}
            </if>
            <if test="startTime != null">
                AND bridge_log.last_attempt_time <![CDATA[>=]]> #{startTime}
            </if>
            <if test="endTime != null">
                AND bridge_log.last_attempt_time <![CDATA[<=]]> #{endTime}
            </if>
            <if test="messageType != null and messageType != ''">
                AND message.message_type = #{messageType}
            </if>
            <if test="sourceType != null and sourceType != ''">
                AND message.source_type = #{sourceType}
            </if>
            <if test="priority != null and priority != ''">
                AND message.priority = #{priority}
            </if>
            <if test="channelCode != null and channelCode != ''">
                AND bridge_log.channel_code = #{channelCode}
            </if>
            <if test="bridgeStatus != null">
                AND bridge_log.bridge_status = #{bridgeStatus}
            </if>
            </script>
            """)
    Long countBridgeLogs(@Param("startTime") Date startTime,
                         @Param("endTime") Date endTime,
                         @Param("messageType") String messageType,
                         @Param("sourceType") String sourceType,
                         @Param("priority") String priority,
                         @Param("channelCode") String channelCode,
                         @Param("bridgeStatus") Integer bridgeStatus,
                         @Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT
                bridge_log.id,
                bridge_log.message_id AS messageId,
                message.title,
                message.message_type AS messageType,
                message.priority,
                message.source_type AS sourceType,
                message.source_id AS sourceId,
                message.related_path AS relatedPath,
                DATE_FORMAT(message.publish_time, '%Y-%m-%d %H:%i:%s') AS publishTime,
                bridge_log.channel_code AS channelCode,
                channel.channel_name AS channelName,
                channel.channel_type AS channelType,
                bridge_log.bridge_scene AS bridgeScene,
                bridge_log.bridge_status AS bridgeStatus,
                bridge_log.unread_count AS unreadCount,
                bridge_log.attempt_count AS attemptCount,
                DATE_FORMAT(bridge_log.last_attempt_time, '%Y-%m-%d %H:%i:%s') AS lastAttemptTime,
                DATE_FORMAT(bridge_log.success_time, '%Y-%m-%d %H:%i:%s') AS successTime,
                bridge_log.response_status_code AS responseStatusCode,
                bridge_log.response_body AS responseBody
            FROM sys_in_app_message_bridge_log bridge_log
            LEFT JOIN sys_in_app_message message
                ON message.id = bridge_log.message_id
            LEFT JOIN sys_notification_channel channel
                ON channel.channel_code = bridge_log.channel_code
                AND (channel.deleted = 0 OR channel.deleted IS NULL)
            WHERE bridge_log.bridge_scene = 'in_app_unread_bridge'
            <if test="tenantId != null">
                AND bridge_log.tenant_id = #{tenantId}
            </if>
            <if test="startTime != null">
                AND bridge_log.last_attempt_time <![CDATA[>=]]> #{startTime}
            </if>
            <if test="endTime != null">
                AND bridge_log.last_attempt_time <![CDATA[<=]]> #{endTime}
            </if>
            <if test="messageType != null and messageType != ''">
                AND message.message_type = #{messageType}
            </if>
            <if test="sourceType != null and sourceType != ''">
                AND message.source_type = #{sourceType}
            </if>
            <if test="priority != null and priority != ''">
                AND message.priority = #{priority}
            </if>
            <if test="channelCode != null and channelCode != ''">
                AND bridge_log.channel_code = #{channelCode}
            </if>
            <if test="bridgeStatus != null">
                AND bridge_log.bridge_status = #{bridgeStatus}
            </if>
            ORDER BY bridge_log.last_attempt_time DESC, bridge_log.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<InAppMessageBridgeLogVO> pageBridgeLogs(@Param("startTime") Date startTime,
                                                 @Param("endTime") Date endTime,
                                                 @Param("messageType") String messageType,
                                                 @Param("sourceType") String sourceType,
                                                 @Param("priority") String priority,
                                                 @Param("channelCode") String channelCode,
                                                 @Param("bridgeStatus") Integer bridgeStatus,
                                                 @Param("tenantId") Long tenantId,
                                                 @Param("offset") long offset,
                                                 @Param("limit") long limit);

    @Select("""
            <script>
            SELECT
                bridge_log.id,
                bridge_log.message_id AS messageId,
                message.title,
                message.message_type AS messageType,
                message.priority,
                message.source_type AS sourceType,
                message.source_id AS sourceId,
                message.related_path AS relatedPath,
                DATE_FORMAT(message.publish_time, '%Y-%m-%d %H:%i:%s') AS publishTime,
                bridge_log.channel_code AS channelCode,
                channel.channel_name AS channelName,
                channel.channel_type AS channelType,
                bridge_log.bridge_scene AS bridgeScene,
                bridge_log.bridge_status AS bridgeStatus,
                bridge_log.unread_count AS unreadCount,
                bridge_log.attempt_count AS attemptCount,
                DATE_FORMAT(bridge_log.last_attempt_time, '%Y-%m-%d %H:%i:%s') AS lastAttemptTime,
                DATE_FORMAT(bridge_log.success_time, '%Y-%m-%d %H:%i:%s') AS successTime,
                bridge_log.response_status_code AS responseStatusCode,
                bridge_log.response_body AS responseBody
            FROM sys_in_app_message_bridge_log bridge_log
            LEFT JOIN sys_in_app_message message
                ON message.id = bridge_log.message_id
            LEFT JOIN sys_notification_channel channel
                ON channel.channel_code = bridge_log.channel_code
                AND (channel.deleted = 0 OR channel.deleted IS NULL)
            WHERE bridge_log.bridge_scene = 'in_app_unread_bridge'
            <if test="tenantId != null">
                AND bridge_log.tenant_id = #{tenantId}
            </if>
            <if test="startTime != null">
                AND bridge_log.last_attempt_time <![CDATA[>=]]> #{startTime}
            </if>
            <if test="endTime != null">
                AND bridge_log.last_attempt_time <![CDATA[<=]]> #{endTime}
            </if>
            <if test="messageType != null and messageType != ''">
                AND message.message_type = #{messageType}
            </if>
            <if test="sourceType != null and sourceType != ''">
                AND message.source_type = #{sourceType}
            </if>
            <if test="priority != null and priority != ''">
                AND message.priority = #{priority}
            </if>
            <if test="channelCode != null and channelCode != ''">
                AND bridge_log.channel_code = #{channelCode}
            </if>
            <if test="bridgeStatus != null">
                AND bridge_log.bridge_status = #{bridgeStatus}
            </if>
            ORDER BY bridge_log.last_attempt_time DESC, bridge_log.id DESC
            </script>
            """)
    List<InAppMessageBridgeLogVO> listBridgeLogsForStats(@Param("startTime") Date startTime,
                                                         @Param("endTime") Date endTime,
                                                         @Param("messageType") String messageType,
                                                         @Param("sourceType") String sourceType,
                                                         @Param("priority") String priority,
                                                         @Param("channelCode") String channelCode,
                                                         @Param("bridgeStatus") Integer bridgeStatus,
                                                         @Param("tenantId") Long tenantId);
}
