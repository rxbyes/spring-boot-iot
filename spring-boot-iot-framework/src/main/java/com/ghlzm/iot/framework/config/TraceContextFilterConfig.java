package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.observability.TraceContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * traceId 过滤器注册。
 */
@Configuration
public class TraceContextFilterConfig {

    @Bean
    public FilterRegistrationBean<TraceContextFilter> traceContextFilterRegistration() {
        FilterRegistrationBean<TraceContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceContextFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(5);
        registration.setName("traceContextFilter");
        return registration;
    }
}
