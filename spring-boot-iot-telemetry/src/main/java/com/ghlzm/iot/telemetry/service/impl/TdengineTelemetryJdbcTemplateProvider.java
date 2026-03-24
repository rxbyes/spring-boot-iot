package com.ghlzm.iot.telemetry.service.impl;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * TDengine 专用 JdbcTemplate 提供器。
 */
@Component
public class TdengineTelemetryJdbcTemplateProvider {

    private static final String PREFIX = "spring.datasource.dynamic.datasource.slave_1.";

    private final Environment environment;

    private volatile JdbcTemplate jdbcTemplate;

    public TdengineTelemetryJdbcTemplateProvider(Environment environment) {
        this.environment = environment;
    }

    public JdbcTemplate getJdbcTemplate() {
        if (jdbcTemplate != null) {
            return jdbcTemplate;
        }
        synchronized (this) {
            if (jdbcTemplate == null) {
                jdbcTemplate = new JdbcTemplate(buildDataSource());
            }
            return jdbcTemplate;
        }
    }

    private DriverManagerDataSource buildDataSource() {
        String url = requireProperty("url");
        String driverClassName = environment.getProperty(PREFIX + "driver-class-name");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        if (StringUtils.hasText(driverClassName)) {
            dataSource.setDriverClassName(driverClassName.trim());
        }
        dataSource.setUrl(url);
        dataSource.setUsername(environment.getProperty(PREFIX + "username"));
        dataSource.setPassword(environment.getProperty(PREFIX + "password"));
        return dataSource;
    }

    private String requireProperty(String key) {
        String value = environment.getProperty(PREFIX + key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("缺少 TDengine 数据源配置: " + PREFIX + key);
        }
        return value.trim();
    }
}
