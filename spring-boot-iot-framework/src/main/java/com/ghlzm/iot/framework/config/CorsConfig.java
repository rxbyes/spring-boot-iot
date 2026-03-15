package com.ghlzm.iot.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS 配置。
 * 允许前端开发时直连后端 API，同时保留可配置扩展点。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(IotProperties iotProperties) {
        IotProperties.Cors cors = iotProperties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();

        if (Boolean.FALSE.equals(cors.getEnabled())) {
            configuration.setAllowedOriginPatterns(java.util.List.of());
        } else {
            configuration.setAllowedOriginPatterns(cors.getAllowedOriginPatterns());
        }

        configuration.setAllowedMethods(cors.getAllowedMethods());
        configuration.setAllowedHeaders(cors.getAllowedHeaders());
        configuration.setAllowCredentials(cors.getAllowCredentials());
        configuration.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
