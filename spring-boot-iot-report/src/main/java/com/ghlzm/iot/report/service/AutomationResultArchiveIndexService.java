package com.ghlzm.iot.report.service;

import com.ghlzm.iot.report.vo.AutomationResultArchiveFacetVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveIndexVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveRefreshVO;

/**
 * 自动化结果归档索引服务。
 */
public interface AutomationResultArchiveIndexService {

    /**
     * 读取归档索引；缺失或过期时自动刷新。
     */
    AutomationResultArchiveIndexVO loadArchiveIndex(boolean forceRefresh);

    /**
     * 查询可用筛选维度。
     */
    AutomationResultArchiveFacetVO listFacets();

    /**
     * 手动刷新归档索引。
     */
    AutomationResultArchiveRefreshVO refreshIndex();
}
