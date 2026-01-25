package com.ecommerce.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            template.header("X-Auth-User-Id", request.getHeader("X-Auth-User-Id"));
            template.header("X-Auth-User-Role", request.getHeader("X-Auth-User-Role"));
            template.header("X-Auth-Token", request.getHeader("X-Auth-Token"));
        }
    }
}