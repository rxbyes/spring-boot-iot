package com.ghlzm.iot.common.response;

import lombok.Data;

import java.util.List;

/**
 * 分页返回类
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:35
 */
@Data
public class PageResult<T> {

    private Long total;

    private Long pageNum;

    private Long pageSize;

    private List<T> records;
}

