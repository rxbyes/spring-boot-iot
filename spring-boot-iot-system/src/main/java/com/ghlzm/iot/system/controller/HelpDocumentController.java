package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.service.HelpDocumentService;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;
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

    public HelpDocumentController(HelpDocumentService helpDocumentService) {
        this.helpDocumentService = helpDocumentService;
    }

    @GetMapping("/page")
    public R<PageResult<HelpDocument>> pageDocuments(@RequestParam(required = false) String title,
                                                     @RequestParam(required = false) String docCategory,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") Long pageNum,
                                                     @RequestParam(defaultValue = "10") Long pageSize) {
        return R.ok(helpDocumentService.pageDocuments(title, docCategory, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<HelpDocument> getById(@PathVariable Long id) {
        return R.ok(helpDocumentService.getById(id));
    }

    @PostMapping("/add")
    public R<HelpDocument> addDocument(@RequestBody HelpDocument document, Authentication authentication) {
        return R.ok(helpDocumentService.addDocument(document, requireCurrentUserId(authentication)));
    }

    @PutMapping("/update")
    public R<Void> updateDocument(@RequestBody HelpDocument document, Authentication authentication) {
        helpDocumentService.updateDocument(document, requireCurrentUserId(authentication));
        return R.ok();
    }

    @DeleteMapping("/delete/{id}")
    public R<Void> deleteDocument(@PathVariable Long id, Authentication authentication) {
        helpDocumentService.deleteDocument(id, requireCurrentUserId(authentication));
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

    @GetMapping("/access/{id}")
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
}
