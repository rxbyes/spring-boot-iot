package com.ghlzm.iot.system.config;

import com.ghlzm.iot.system.filter.AuditLogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计日志过滤器注册配置。
 */
@Configuration
public class AuditLogFilterConfig {

    @Bean
    public FilterRegistrationBean<AuditLogFilter> auditLogFilterRegistration(AuditLogFilter auditLogFilter) {
        FilterRegistrationBean<AuditLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(auditLogFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(10);
        registration.setName("auditLogFilter");
        return registration;
    }
}
