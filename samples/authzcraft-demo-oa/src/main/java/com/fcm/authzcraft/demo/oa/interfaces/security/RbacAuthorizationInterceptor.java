package com.fcm.authzcraft.demo.oa.interfaces.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.demo.oa.application.service.RbacApplicationService;
import com.fcm.authzcraft.demo.oa.domain.model.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class RbacAuthorizationInterceptor implements HandlerInterceptor {

    public static final String REQUESTER_KIND_HEADER = "X-AuthzCraft-Requester-Kind";
    public static final String REQUESTER_KEY_HEADER = "X-AuthzCraft-Requester-Key";
    public static final String REQUEST_KEY_HEADER = "X-AuthzCraft-Request-Key";

    private final RbacApplicationService rbacApplicationService;
    private final ObjectMapper objectMapper;

    public RbacAuthorizationInterceptor(RbacApplicationService rbacApplicationService, ObjectMapper objectMapper) {
        this.rbacApplicationService = rbacApplicationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        CurrentUserContext.clear();
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String requestKey = resolveRequestKey(request);
        request.setAttribute(REQUEST_KEY_HEADER, requestKey);
        response.setHeader(REQUEST_KEY_HEADER, requestKey);

        CurrentUser currentUser = rbacApplicationService.resolveCurrentUser(request.getHeader(REQUESTER_KEY_HEADER), requestKey);
        if (currentUser == null) {
            writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "DEMO_OA_UNAUTHENTICATED", "Requester header is missing or unknown");
            return false;
        }
        CurrentUserContext.set(currentUser);

        RequirePermission requirePermission = resolveRequirePermission((HandlerMethod) handler);
        if (requirePermission != null && !currentUser.hasPermission(requirePermission.value())) {
            writeFailure(response, HttpServletResponse.SC_FORBIDDEN, "DEMO_OA_FORBIDDEN", "Permission denied: " + requirePermission.value());
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private RequirePermission resolveRequirePermission(HandlerMethod handlerMethod) {
        RequirePermission methodPermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (methodPermission != null) {
            return methodPermission;
        }
        return handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
    }

    private String resolveRequestKey(HttpServletRequest request) {
        String requestKey = request.getHeader(REQUEST_KEY_HEADER);
        if (StringUtils.hasText(requestKey)) {
            return requestKey.trim();
        }
        return "rbac-" + UUID.randomUUID().toString();
    }

    private void writeFailure(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(code, message)));
        response.getWriter().flush();
    }
}