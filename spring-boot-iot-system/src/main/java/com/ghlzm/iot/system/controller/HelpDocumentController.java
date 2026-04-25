package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.HelpDocumentService;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

@RestController
@RequestMapping("/api/system/help-doc")
public class HelpDocumentController {

    private final HelpDocumentService helpDocumentService;
    private final GovernancePermissionGuard permissionGuard;

    public HelpDocumentController(HelpDocumentService helpDocumentService) {
        this(helpDocumentService, null);
    }

    @Autowired
    public HelpDocumentController(HelpDocumentService helpDocumentService, GovernancePermissionGuard permissionGuard) {
        this.helpDocumentService = helpDocumentService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/page")
    public R<PageResult<HelpDocument>> pageDocuments(@RequestParam(required = false) String title,
                                                     @RequestParam(required = false) String docCategory,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") Long pageNum,
                                                     @RequestParam(defaultValue = "10") Long pageSize,
                                                     Authentication authentication) {
        return R.ok(helpDocumentService.pageDocuments(requireCurrentUserId(authentication), title, docCategory, status, pageNum, pageSize));
    }

    @GetMapping("/{id:[0-9]+}")
    public R<HelpDocument> getById(@PathVariable Long id, Authentication authentication) {
        return R.ok(helpDocumentService.getById(requireCurrentUserId(authentication), id));
    }

    @PostMapping("/add")
    public R<HelpDocument> addDocument(@RequestBody HelpDocument document, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "新增帮助文档", GovernancePermissionCodes.HELP_DOC_ADD);
        return R.ok(helpDocumentService.addDocument(document, currentUserId));
    }

    @PutMapping("/update")
    public R<Void> updateDocument(@RequestBody HelpDocument document, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "编辑帮助文档", GovernancePermissionCodes.HELP_DOC_UPDATE);
        helpDocumentService.updateDocument(document, currentUserId);
        return R.ok();
    }

    @DeleteMapping("/delete/{id:[0-9]+}")
    public R<Void> deleteDocument(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = requireCurrentUserId(authentication);
        requirePermission(currentUserId, "删除帮助文档", GovernancePermissionCodes.HELP_DOC_DELETE);
        helpDocumentService.deleteDocument(id, currentUserId);
        return R.ok();
    }

    @GetMapping("/access/list")
    public R<List<HelpDocumentAccessVO>> listAccessibleDocuments(@RequestParam(required = false) String docCategory,
                                                                 @RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String currentPath,
                                                                 @RequestParam(required = false) Integer limit,
                                                                 Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        return R.ok(helpDocumentService.listAccessibleDocuments(userId, docCategory, keyword, currentPath, limit));
    }

    @GetMapping("/access/page")
    public R<PageResult<HelpDocumentAccessVO>> pageAccessibleDocuments(@RequestParam(required = false) String docCategory,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String currentPath,
                                                                       @RequestParam(defaultValue = "1") Long pageNum,
                                                                       @RequestParam(defaultValue = "10") Long pageSize,
                                                                       Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        return R.ok(helpDocumentService.pageAccessibleDocuments(userId, docCategory, keyword, currentPath, pageNum, pageSize));
    }

    @GetMapping("/access/{id:[0-9]+}")
    public R<HelpDocumentAccessVO> getAccessibleDocument(@PathVariable Long id,
                                                         @RequestParam(required = false) String currentPath,
                                                         Authentication authentication) {
        return R.ok(helpDocumentService.getAccessibleDocument(requireCurrentUserId(authentication), id, currentPath));
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }

    private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
        if (permissionGuard != null) {
            permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
        }
    }
}
