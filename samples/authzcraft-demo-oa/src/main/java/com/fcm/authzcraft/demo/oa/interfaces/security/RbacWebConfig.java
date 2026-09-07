package com.fcm.authzcraft.demo.oa.interfaces.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RbacWebConfig implements WebMvcConfigurer {

    private final RbacAuthorizationInterceptor authorizationInterceptor;

    public RbacWebConfig(RbacAuthorizationInterceptor authorizationInterceptor) {
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/authzcraft-demo-oa/api/v1/**");
    }
}