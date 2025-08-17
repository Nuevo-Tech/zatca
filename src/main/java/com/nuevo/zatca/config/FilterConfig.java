package com.nuevo.zatca.config;

import com.nuevo.zatca.filter.InputSanitizationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<InputSanitizationFilter> inputSanitizationFilter() {
        FilterRegistrationBean<InputSanitizationFilter> registrationBean =
                new FilterRegistrationBean<>();
        registrationBean.setFilter(new InputSanitizationFilter());
        registrationBean.addUrlPatterns("/*"); // Apply to all requests
        registrationBean.setOrder(1); // High priority
        return registrationBean;
    }
}
