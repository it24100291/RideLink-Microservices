package com.ridelink.payment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PaymentSecurityConfiguration implements WebMvcConfigurer {
    private final String serviceKey;

    public PaymentSecurityConfiguration(@Value("${payment.internal.service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PaymentServiceKeyInterceptor(serviceKey))
                .addPathPatterns("/api/payments/**", "/api/fares/**");
    }
}
