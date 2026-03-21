package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.mapper.HelpDocumentMapper;
import com.ghlzm.iot.system.service.HelpDocumentService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HelpDocumentServiceImpl extends ServiceImpl<HelpDocumentMapper, HelpDocument> implements HelpDocumentService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("business", "technical", "faq");

    private final HelpDocumentMapper helpDocumentMapper;
    private final PermissionService permissionService;
    private final SystemContentSchemaSupport systemContentSchemaSupport;

    public HelpDocumentServiceImpl(HelpDocumentMapper helpDocumentMapper,
                                   PermissionService permissionService,
                                   SystemContentSchemaSupport systemContentSchemaSupport) {
        this.helpDocumentMapper = helpDocumentMapper;
        this.permissionService = permissionService;
        this.systemContentSchemaSupport = systemContentSchemaSupport;
    }

    @Override
    public HelpDocument getById(Serializable id) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HelpDocument addDocument(HelpDocument document, Long operatorId) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        normalizeAndValidateDocument(document, null);
        if (document.getCreateBy() == null) {
            document.setCreateBy(defaultOperator(operatorId));
        }
        if (document.getUpdateBy() == null) {
            document.setUpdateBy(defaultOperator(operatorId));
        }
        helpDocumentMapper.insert(document);
        return helpDocumentMapper.selectById(document.getId());
    }

    @Override
    public PageResult<HelpDocument> pageDocuments(String title,
                                                  String docCategory,
                                                  Integer status,
                                                  Long pageNum,
                                                  Long pageSize) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        Page<HelpDocument> page = PageQueryUtils.buildPage(pageNum, pageSize);
        LambdaQueryWrapper<HelpDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HelpDocument::getDeleted, 0);
        if (StringUtils.hasText(title)) {
            queryWrapper.and(wrapper -> wrapper.like(HelpDocument::getTitle, title.trim())
                    .or()
                    .like(HelpDocument::getSummary, title.trim()));
        }
        if (StringUtils.hasText(docCategory)) {
            queryWrapper.eq(HelpDocument::getDocCategory, docCategory.trim().toLowerCase(Locale.ROOT));
        }
        if (status != null) {
            queryWrapper.eq(HelpDocument::getStatus, status);
        }
        queryWrapper.orderByAsc(HelpDocument::getSortNo)
                .orderByDesc(HelpDocument::getUpdateTime)
                .orderByDesc(HelpDocument::getId);
        return PageQueryUtils.toPageResult(helpDocumentMapper.selectPage(page, queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(HelpDocument document, Long operatorId) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        HelpDocument existing = requireDocument(document == null ? null : document.getId());
        normalizeAndValidateDocument(document, existing);
        document.setTenantId(existing.getTenantId());
        document.setUpdateBy(defaultOperator(operatorId));
        helpDocumentMapper.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id, Long operatorId) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        HelpDocument existing = requireDocument(id);
        existing.setDeleted(1);
        existing.setUpdateBy(defaultOperator(operatorId));
        helpDocumentMapper.updateById(existing);
    }

    @Override
    public List<HelpDocumentAccessVO> listAccessibleDocuments(Long userId,
                                                              String docCategory,
                                                              String keyword,
                                                              String currentPath,
                                                              Integer limit) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        List<HelpDocumentAccessVO> documents = listAccessibleDocumentRecords(userId, docCategory, keyword, currentPath);
        if (limit == null || limit <= 0 || documents.size() <= limit) {
            return documents;
        }
        return documents.subList(0, limit);
    }

    @Override
    public PageResult<HelpDocumentAccessVO> pageAccessibleDocuments(Long userId,
                                                                    String docCategory,
                                                                    String keyword,
                                                                    String currentPath,
                                                                    Long pageNum,
                                                                    Long pageSize) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        long safePageNum = PageQueryUtils.normalizePageNum(pageNum);
        long safePageSize = PageQueryUtils.normalizePageSize(pageSize);
        List<HelpDocumentAccessVO> documents = listAccessibleDocumentRecords(userId, docCategory, keyword, currentPath);
        if (documents.isEmpty()) {
            return PageResult.empty(safePageNum, safePageSize);
        }

        int fromIndex = (int) Math.min((safePageNum - 1L) * safePageSize, documents.size());
        int toIndex = (int) Math.min(fromIndex + safePageSize, documents.size());
        return PageResult.of((long) documents.size(), safePageNum, safePageSize, documents.subList(fromIndex, toIndex));
    }

    private List<HelpDocumentAccessVO> listAccessibleDocumentRecords(Long userId,
                                                                     String docCategory,
                                                                     String keyword,
                                                                     String currentPath) {
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        AccessScope scope = buildAccessScope(authContext, currentPath);
        String normalizedCategory = StringUtils.hasText(docCategory) ? docCategory.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        return helpDocumentMapper.selectList(buildAccessQueryWrapper()).stream()
                .filter(document -> !StringUtils.hasText(normalizedCategory) || normalizedCategory.equals(document.getDocCategory()))
                .filter(document -> matchesRoleScope(document, scope.roleCodes()))
                .filter(document -> matchesPermissionScope(document, scope.authorizedPaths()))
                .filter(document -> matchesKeyword(document, normalizedKeyword))
                .map(document -> toAccessVO(document, scope.currentPath()))
                .sorted(accessComparator())
                .toList();
    }

    @Override
    public HelpDocumentAccessVO getAccessibleDocument(Long userId, Long id, String currentPath) {
        systemContentSchemaSupport.ensureHelpDocumentReady();
        HelpDocument document = requireDocument(id);
        if (!Integer.valueOf(1).equals(document.getStatus())) {
            throw new BizException("帮助文档不存在");
        }
        UserAuthContextVO authContext = permissionService.getUserAuthContext(userId);
        AccessScope scope = buildAccessScope(authContext, currentPath);
        if (!matchesRoleScope(document, scope.roleCodes()) || !matchesPermissionScope(document, scope.authorizedPaths())) {
            throw new BizException("帮助文档不存在");
        }
        return toAccessVO(document, scope.currentPath());
    }

    private void normalizeAndValidateDocument(HelpDocument document, HelpDocument existing) {
        if (document == null) {
            throw new BizException("帮助文档不能为空");
        }
        document.setTenantId(existing == null ? defaultTenantId(document.getTenantId()) : existing.getTenantId());
        document.setDocCategory(normalizeCategory(document.getDocCategory()));
        document.setTitle(requireText(document.getTitle(), "文档标题"));
        document.setSummary(nullableText(document.getSummary()));
        document.setContent(requireText(document.getContent(), "文档正文"));
        document.setKeywords(SystemContentAccessSupport.normalizeCsv(document.getKeywords()));
        document.setRelatedPaths(SystemContentAccessSupport.normalizePathCsv(document.getRelatedPaths()));
        document.setVisibleRoleCodes(SystemContentAccessSupport.normalizeUpperCaseCsv(document.getVisibleRoleCodes()));
        if (document.getStatus() == null) {
            document.setStatus(existing == null ? 1 : existing.getStatus());
        }
        if (document.getSortNo() == null) {
            document.setSortNo(existing == null ? 0 : existing.getSortNo());
        }
    }

    private LambdaQueryWrapper<HelpDocument> buildAccessQueryWrapper() {
        LambdaQueryWrapper<HelpDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HelpDocument::getDeleted, 0)
                .eq(HelpDocument::getStatus, 1)
                .orderByAsc(HelpDocument::getSortNo)
                .orderByDesc(HelpDocument::getUpdateTime)
                .orderByDesc(HelpDocument::getId);
        return queryWrapper;
    }

    private boolean matchesRoleScope(HelpDocument document, Set<String> roleCodes) {
        List<String> visibleRoles = SystemContentAccessSupport.splitCsv(document.getVisibleRoleCodes()).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .toList();
        return visibleRoles.isEmpty() || visibleRoles.stream().anyMatch(roleCodes::contains);
    }

    private boolean matchesPermissionScope(HelpDocument document, Set<String> authorizedPaths) {
        List<String> relatedPaths = SystemContentAccessSupport.normalizePathList(document.getRelatedPaths());
        if (relatedPaths.isEmpty()) {
            return true;
        }
        if (authorizedPaths.isEmpty()) {
            return false;
        }
        return relatedPaths.stream()
                .anyMatch(relatedPath -> authorizedPaths.stream().anyMatch(path -> SystemContentAccessSupport.pathMatches(relatedPath, path)));
    }

    private boolean matchesKeyword(HelpDocument document, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(document.getTitle(), keyword)
                || containsIgnoreCase(document.getSummary(), keyword)
                || containsIgnoreCase(document.getContent(), keyword)
                || containsIgnoreCase(document.getKeywords(), keyword);
    }

    private HelpDocumentAccessVO toAccessVO(HelpDocument document, String currentPath) {
        HelpDocumentAccessVO vo = new HelpDocumentAccessVO();
        vo.setId(document.getId());
        vo.setDocCategory(document.getDocCategory());
        vo.setSortNo(document.getSortNo());
        vo.setTitle(document.getTitle());
        vo.setSummary(document.getSummary());
        vo.setContent(document.getContent());
        vo.setKeywords(document.getKeywords());
        vo.setRelatedPaths(document.getRelatedPaths());
        vo.setKeywordList(SystemContentAccessSupport.splitCsv(document.getKeywords()));
        vo.setRelatedPathList(SystemContentAccessSupport.normalizePathList(document.getRelatedPaths()));
        vo.setCurrentPathMatched(vo.getRelatedPathList().stream()
                .anyMatch(relatedPath -> SystemContentAccessSupport.pathMatches(relatedPath, currentPath)));
        return vo;
    }

    private Comparator<HelpDocumentAccessVO> accessComparator() {
        return Comparator.comparing(HelpDocumentAccessVO::isCurrentPathMatched).reversed()
                .thenComparing(HelpDocumentAccessVO::getSortNo, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HelpDocumentAccessVO::getDocCategory, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HelpDocumentAccessVO::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HelpDocumentAccessVO::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private AccessScope buildAccessScope(UserAuthContextVO authContext, String currentPath) {
        Set<String> roleCodes = SystemContentAccessSupport.toUpperCaseSet(authContext.getRoleCodes());
        Set<String> authorizedPaths = new LinkedHashSet<>();
        collectMenuPaths(authContext.getMenus(), authorizedPaths);
        String normalizedCurrentPath = SystemContentAccessSupport.normalizePath(currentPath);
        return new AccessScope(roleCodes, authorizedPaths, normalizedCurrentPath);
    }

    private void collectMenuPaths(List<MenuTreeNodeVO> menus, Set<String> target) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        for (MenuTreeNodeVO menu : menus) {
            if (menu == null) {
                continue;
            }
            String normalizedPath = SystemContentAccessSupport.normalizePath(menu.getPath());
            if (StringUtils.hasText(normalizedPath)) {
                target.add(normalizedPath);
            }
            collectMenuPaths(menu.getChildren(), target);
        }
    }

    private HelpDocument requireDocument(Long id) {
        if (id == null) {
            throw new BizException("帮助文档不存在");
        }
        HelpDocument document = helpDocumentMapper.selectById(id);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BizException("帮助文档不存在");
        }
        return document;
    }

    private String normalizeCategory(String rawCategory) {
        String normalized = StringUtils.hasText(rawCategory) ? rawCategory.trim().toLowerCase(Locale.ROOT) : null;
        if (!StringUtils.hasText(normalized) || !ALLOWED_CATEGORIES.contains(normalized)) {
            throw new BizException("文档分类不合法");
        }
        return normalized;
    }

    private String requireText(String raw, String fieldName) {
        String normalized = nullableText(raw);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String nullableText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private Long defaultOperator(Long operatorId) {
        return operatorId == null ? 1L : operatorId;
    }

    private record AccessScope(Set<String> roleCodes,
                               Set<String> authorizedPaths,
                               String currentPath) {
    }
}
