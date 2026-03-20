package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.security.JwtAuthenticationFilter;
import com.ghlzm.iot.framework.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:45
 */
@Configuration
public class SecurityConfig {

    private static final RegexRequestMatcher SPA_SHELL_ROUTE_MATCHER =
            new RegexRequestMatcher(
                    "^/(?:$|(?!api(?:/|$)|actuator(?:/|$)|swagger-ui(?:/|$)|v3(?:/|$)|error(?:/|$)|assets(?:/|$)|doc\\.html$)(?!.*\\.[^/]+$).+)$",
                    HttpMethod.GET.name());

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 放行静态资源与 SPA 壳层入口，确保浏览器强刷前端 history 路由时先回到 index.html。
                        .requestMatchers(
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**")
                        .permitAll()
                        .requestMatchers(SPA_SHELL_ROUTE_MATCHER).permitAll()
                        // 仅保留登录、调试与运维白名单，其余接口默认要求 JWT 认证。
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/message/http/report",
                                "/actuator/**",
                                "/error",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/cockpit/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
