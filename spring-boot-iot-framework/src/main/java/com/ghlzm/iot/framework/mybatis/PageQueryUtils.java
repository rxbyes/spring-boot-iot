package com.ghlzm.iot.framework.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/17 - 15:20
 */
public final class PageQueryUtils {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private PageQueryUtils() {
    }

    /**
     * 统一收敛分页参数，避免各业务模块重复处理默认值和上限。
     */
    public static <T> Page<T> buildPage(Number pageNum, Number pageSize) {
        return new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
    }

    public static long normalizePageNum(Number pageNum) {
        if (pageNum == null || pageNum.longValue() < 1L) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum.longValue();
    }

    public static long normalizePageSize(Number pageSize) {
        if (pageSize == null || pageSize.longValue() < 1L) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize.longValue(), MAX_PAGE_SIZE);
    }

    public static <T> PageResult<T> toPageResult(IPage<T> page) {
        if (page == null) {
            return PageResult.empty(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE);
        }
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
