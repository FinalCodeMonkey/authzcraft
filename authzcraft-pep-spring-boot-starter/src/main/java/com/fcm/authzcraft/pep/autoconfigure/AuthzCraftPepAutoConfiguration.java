package com.fcm.authzcraft.pep.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.pep.runtime.AuthzCraftPepInterceptor;
import com.fcm.authzcraft.pep.runtime.AuthzCraftPlanClient;
import com.fcm.authzcraft.pep.runtime.AuthzCraftRequesterResolver;
import com.fcm.authzcraft.pep.runtime.DefaultAuthzCraftPlanClient;
import com.fcm.authzcraft.pep.runtime.DefaultAuthzCraftRequesterResolver;
import com.fcm.authzcraft.pep.runtime.RowFilterSqlCompiler;

import org.apache.ibatis.plugin.Interceptor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnClass(Interceptor.class)
@EnableConfigurationProperties(AuthzCraftPepProperties.class)
@ConditionalOnProperty(prefix = "authzcraft.pep", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthzCraftPepAutoConfiguration {

    @Bean
    public InitializingBean authzCraftPepPropertiesValidator(AuthzCraftPepProperties properties) {
        return new AuthzCraftPepPropertiesValidator(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate authzCraftPepRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public RowFilterSqlCompiler authzCraftRowFilterSqlCompiler() {
        return new RowFilterSqlCompiler();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzCraftPlanClient authzCraftPlanClient(AuthzCraftPepProperties properties,
                                                    RestTemplate authzCraftPepRestTemplate,
                                                    ObjectMapper objectMapper) {
        return new DefaultAuthzCraftPlanClient(properties, authzCraftPepRestTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzCraftRequesterResolver authzCraftRequesterResolver(AuthzCraftPepProperties properties) {
        return new DefaultAuthzCraftRequesterResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzCraftPepInterceptor authzCraftPepInterceptor(AuthzCraftPepProperties properties,
                                                            AuthzCraftPlanClient planClient,
                                                            AuthzCraftRequesterResolver requesterResolver,
                                                            RowFilterSqlCompiler sqlCompiler) {
        return new AuthzCraftPepInterceptor(properties, planClient, requesterResolver, sqlCompiler);
    }
}