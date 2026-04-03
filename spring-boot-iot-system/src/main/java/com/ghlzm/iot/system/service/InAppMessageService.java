package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageStatsVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;

import java.util.Date;

public interface InAppMessageService extends IService<InAppMessage> {

    InAppMessage addMessage(InAppMessage message, Long operatorId);

    default PageResult<InAppMessage> pageMessages(String title,
                                                  String messageType,
                                                  String priority,
                                                  String sourceType,
                                                  String targetType,
                                                  Integer status,
                                                  Long pageNum,
                                                  Long pageSize) {
        return pageMessages(null, title, messageType, priority, sourceType, targetType, status, pageNum, pageSize);
    }

    PageResult<InAppMessage> pageMessages(Long currentUserId,
                                          String title,
                                          String messageType,
                                          String priority,
                                          String sourceType,
                                          String targetType,
                                          Integer status,
                                          Long pageNum,
                                          Long pageSize);

    void updateMessage(InAppMessage message, Long operatorId);

    void deleteMessage(Long id, Long operatorId);

    InAppMessage getById(Long currentUserId, Long id);

    PageResult<InAppMessageAccessVO> pageMyMessages(Long userId,
                                                    String messageType,
                                                    Boolean unreadOnly,
                                                    Long pageNum,
                                                    Long pageSize);

    InAppMessageUnreadStatsVO getMyUnreadStats(Long userId);

    InAppMessageAccessVO getMyMessageDetail(Long userId, Long id);

    void markMessageRead(Long userId, Long id);

    void markAllMessagesRead(Long userId);

    default InAppMessageStatsVO getMessageStats(Date startTime,
                                                Date endTime,
                                                String messageType,
                                                String sourceType) {
        return getMessageStats(null, startTime, endTime, messageType, sourceType);
    }

    InAppMessageStatsVO getMessageStats(Long currentUserId,
                                        Date startTime,
                                        Date endTime,
                                        String messageType,
                                        String sourceType);
}
