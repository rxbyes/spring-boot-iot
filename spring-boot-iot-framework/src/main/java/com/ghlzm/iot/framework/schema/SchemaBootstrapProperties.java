package com.ghlzm.iot.framework.schema;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Schema 启动补齐配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "iot.schema.bootstrap")
public class SchemaBootstrapProperties {

    /**
     * 是否启用 MySQL active schema 启动补齐。
     */
    private boolean mysqlEnabled = true;

    /**
     * 是否启用 TDengine schema 启动补齐。
     */
    private boolean tdengineEnabled = true;

    /**
     * 启动补齐失败时是否直接中断启动。
     */
    private boolean failFast = false;
}
