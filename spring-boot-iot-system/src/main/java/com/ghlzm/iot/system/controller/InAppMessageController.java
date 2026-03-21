package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.service.InAppMessageService;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageStatsVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api/system/in-app-message")
public class InAppMessageController {

    private final InAppMessageService inAppMessageService;

    public InAppMessageController(InAppMessageService inAppMessageService) {
        this.inAppMessageService = inAppMessageService;
    }

    @GetMapping("/page")
    public R<PageResult<InAppMessage>> pageMessages(@RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String messageType,
                                                    @RequestParam(required = false) String priority,
                                                    @RequestParam(required = false) String sourceType,
                                                    @RequestParam(required = false) String targetType,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(defaultValue = "1") Long pageNum,
                                                    @RequestParam(defaultValue = "10") Long pageSize) {
        return R.ok(inAppMessageService.pageMessages(title, messageType, priority, sourceType, targetType, status, pageNum, pageSize));
    }

    @GetMapping("/stats")
    public R<InAppMessageStatsVO> getMessageStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String sourceType) {
        return R.ok(inAppMessageService.getMessageStats(startTime, endTime, messageType, sourceType));
    }

    @GetMapping("/{id:[0-9]+}")
    public R<InAppMessage> getById(@PathVariable Long id) {
        return R.ok(inAppMessageService.getById(id));
    }

    @PostMapping("/add")
    public R<InAppMessage> addMessage(@RequestBody InAppMessage message, Authentication authentication) {
        return R.ok(inAppMessageService.addMessage(message, requireCurrentUserId(authentication)));
    }

    @PutMapping("/update")
    public R<Void> updateMessage(@RequestBody InAppMessage message, Authentication authentication) {
        inAppMessageService.updateMessage(message, requireCurrentUserId(authentication));
        return R.ok();
    }

    @DeleteMapping("/delete/{id:[0-9]+}")
    public R<Void> deleteMessage(@PathVariable Long id, Authentication authentication) {
        inAppMessageService.deleteMessage(id, requireCurrentUserId(authentication));
        return R.ok();
    }

    @GetMapping("/my/page")
    public R<PageResult<InAppMessageAccessVO>> pageMyMessages(@RequestParam(required = false) String messageType,
                                                              @RequestParam(required = false) Boolean unreadOnly,
                                                              @RequestParam(defaultValue = "1") Long pageNum,
                                                              @RequestParam(defaultValue = "10") Long pageSize,
                                                              Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        return R.ok(inAppMessageService.pageMyMessages(userId, messageType, unreadOnly, pageNum, pageSize));
    }

    @GetMapping("/my/unread-count")
    public R<InAppMessageUnreadStatsVO> getMyUnreadStats(Authentication authentication) {
        return R.ok(inAppMessageService.getMyUnreadStats(requireCurrentUserId(authentication)));
    }

    @GetMapping("/my/{id:[0-9]+}")
    public R<InAppMessageAccessVO> getMyMessageDetail(@PathVariable Long id, Authentication authentication) {
        return R.ok(inAppMessageService.getMyMessageDetail(requireCurrentUserId(authentication), id));
    }

    @PostMapping("/my/read/{id:[0-9]+}")
    public R<Void> markMessageRead(@PathVariable Long id, Authentication authentication) {
        inAppMessageService.markMessageRead(requireCurrentUserId(authentication), id);
        return R.ok();
    }

    @PostMapping("/my/read-all")
    public R<Void> markAllMessagesRead(Authentication authentication) {
        inAppMessageService.markAllMessagesRead(requireCurrentUserId(authentication));
        return R.ok();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
