package me.kitkas1412.user.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cung cấp bean {@code jwtAuthenticationFilter} cho {@code SecurityConfig} ở module infrastructure.
 *
 * <p>SecurityConfig không phụ thuộc module user nên nhận filter qua kiểu {@code Filter}.
 * Vì filter là bean, Spring Boot sẽ tự đăng ký nó vào servlet filter chain; bean
 * {@link FilterRegistrationBean} với {@code enabled=false} tắt việc đó để filter chỉ chạy
 * một lần bên trong SecurityFilterChain (xem Javadoc của {@link JwtAuthenticationFilter}).
 */
@Configuration(proxyBeanMethods = false)
public class JwtFilterConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
