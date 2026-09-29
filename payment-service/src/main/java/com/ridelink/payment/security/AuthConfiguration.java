package com.ridelink.payment.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthConfiguration implements WebMvcConfigurer {
    private final AccountClient accounts;

    public AuthConfiguration(AccountClient accounts) {
        this.accounts = accounts;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                Identity identity = accounts.authenticate(request.getHeader("Authorization"));
                request.setAttribute("identity", identity);
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
