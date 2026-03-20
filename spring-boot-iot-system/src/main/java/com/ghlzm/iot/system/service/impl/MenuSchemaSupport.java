package com.ghlzm.iot.system.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 菜单表结构支持类，负责缓存真实库当前可用列。
 */
@Slf4j
@Component
public class MenuSchemaSupport {

    private static final String TABLE_NAME = "sys_menu";

    private final JdbcTemplate jdbcTemplate;
    private volatile Set<String> cachedColumns;

    public MenuSchemaSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> getColumns() {
        Set<String> current = cachedColumns;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedColumns == null) {
                cachedColumns = loadColumns();
            }
            return cachedColumns;
        }
    }

    public void refresh() {
        cachedColumns = loadColumns();
    }

    private Set<String> loadColumns() {
        try {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ?",
                    String.class,
                    TABLE_NAME
            );
            Set<String> normalized = new LinkedHashSet<>();
            for (String column : columns) {
                if (column != null && !column.isBlank()) {
                    normalized.add(column.toLowerCase(Locale.ROOT));
                }
            }
            return Collections.unmodifiableSet(normalized);
        } catch (Exception ex) {
            log.warn("加载菜单表结构失败，将继续按实体默认逻辑兜底, error={}", ex.getMessage());
            return Collections.emptySet();
        }
    }
}
