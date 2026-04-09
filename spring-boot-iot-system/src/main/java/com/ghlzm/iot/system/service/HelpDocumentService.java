package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;

import java.util.List;

public interface HelpDocumentService extends IService<HelpDocument> {

    HelpDocument addDocument(HelpDocument document, Long operatorId);

    PageResult<HelpDocument> pageDocuments(Long currentUserId,
                                           String title,
                                           String docCategory,
                                           Integer status,
                                           Long pageNum,
                                           Long pageSize);

    PageResult<HelpDocument> pageDocuments(String title,
                                           String docCategory,
                                           Integer status,
                                           Long pageNum,
                                           Long pageSize);

    HelpDocument getById(Long currentUserId, Long id);

    void updateDocument(HelpDocument document, Long operatorId);

    void deleteDocument(Long id, Long operatorId);

    List<HelpDocumentAccessVO> listAccessibleDocuments(Long userId,
                                                       String docCategory,
                                                       String keyword,
                                                       String currentPath,
                                                       Integer limit);

    PageResult<HelpDocumentAccessVO> pageAccessibleDocuments(Long userId,
                                                             String docCategory,
                                                             String keyword,
                                                             String currentPath,
                                                             Long pageNum,
                                                             Long pageSize);

    HelpDocumentAccessVO getAccessibleDocument(Long userId, Long id, String currentPath);
}
