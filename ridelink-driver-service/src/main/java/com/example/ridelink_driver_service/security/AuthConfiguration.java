package com.example.ridelink_driver_service.security;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class AuthConfiguration implements WebMvcConfigurer {
    private final AccountClient accounts;
    public AuthConfiguration(AccountClient accounts) { this.accounts = accounts; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                request.setAttribute("identity", accounts.authenticate(request.getHeader("Authorization")));
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
