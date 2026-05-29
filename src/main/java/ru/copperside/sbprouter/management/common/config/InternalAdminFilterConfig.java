package ru.copperside.sbprouter.management.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import ru.copperside.sbprouter.management.common.web.InternalAdminApiKeyFilter;
import ru.copperside.sbprouter.management.config.InternalAdminSecurityProperties;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(InternalAdminSecurityProperties.class)
public class InternalAdminFilterConfig {

    @Bean
    public FilterRegistrationBean<InternalAdminApiKeyFilter> internalAdminApiKeyFilter(
            InternalAdminSecurityProperties properties, Clock clock) {
        FilterRegistrationBean<InternalAdminApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InternalAdminApiKeyFilter(properties, clock));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
