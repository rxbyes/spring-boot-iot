package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;

public interface InAppMessageService extends IService<InAppMessage> {

    InAppMessage addMessage(InAppMessage message, Long operatorId);

    PageResult<InAppMessage> pageMessages(String title,
                                          String messageType,
                                          String priority,
                                          String targetType,
                                          Integer status,
                                          Long pageNum,
                                          Long pageSize);

    void updateMessage(InAppMessage message, Long operatorId);

    void deleteMessage(Long id, Long operatorId);

    PageResult<InAppMessageAccessVO> pageMyMessages(Long userId,
                                                    String messageType,
                                                    Boolean unreadOnly,
                                                    Long pageNum,
                                                    Long pageSize);

    InAppMessageUnreadStatsVO getMyUnreadStats(Long userId);

    InAppMessageAccessVO getMyMessageDetail(Long userId, Long id);

    void markMessageRead(Long userId, Long id);

    void markAllMessagesRead(Long userId);
}
