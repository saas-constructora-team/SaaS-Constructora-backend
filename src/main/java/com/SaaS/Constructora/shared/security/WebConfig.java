package com.SaaS.Constructora.shared.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registra la resolución del contexto autenticado en los controladores.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    /**
     * Abre la sesión JPA antes de la cadena de seguridad para que
     * {@link MembershipValidationFilter} pueda habilitar el filtro Hibernate
     * {@code tenantFilter} sobre la misma sesión que usan los controladores.
     */
    @Bean
    FilterRegistrationBean<OpenEntityManagerInViewFilter> openEntityManagerInViewFilter() {
        OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter();
        FilterRegistrationBean<OpenEntityManagerInViewFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
