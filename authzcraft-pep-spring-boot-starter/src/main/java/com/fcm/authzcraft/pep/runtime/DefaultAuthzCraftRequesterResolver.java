package com.fcm.authzcraft.pep.runtime;

import com.fcm.authzcraft.api.common.RequesterKind;
import com.fcm.authzcraft.pep.autoconfigure.AuthzCraftPepProperties;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class DefaultAuthzCraftRequesterResolver implements AuthzCraftRequesterResolver {

    private final AuthzCraftPepProperties properties;

    public DefaultAuthzCraftRequesterResolver(AuthzCraftPepProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthzCraftRequester resolveRequester() {
        if ("HEADER".equalsIgnoreCase(properties.getRequesterSource())) {
            return resolveHeaderRequester();
        }
        if (!StringUtils.hasText(properties.getRequesterKey())) {
            throw new AuthzCraftPepException("authzcraft.pep.requester-key is required");
        }
        return new AuthzCraftRequester(properties.getRequesterKind(), properties.getRequesterKey());
    }

    private AuthzCraftRequester resolveHeaderRequester() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            throw new AuthzCraftPepException("AuthzCraft requester headers are unavailable outside an HTTP request");
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String requesterKey = request.getHeader(resolveHeaderName(properties.getRequesterKeyHeader(), "X-AuthzCraft-Requester-Key"));
        if (!StringUtils.hasText(requesterKey)) {
            throw new AuthzCraftPepException("AuthzCraft requester header is required: "
                    + resolveHeaderName(properties.getRequesterKeyHeader(), "X-AuthzCraft-Requester-Key"));
        }
        String requesterKindValue = request.getHeader(resolveHeaderName(properties.getRequesterKindHeader(), "X-AuthzCraft-Requester-Kind"));
        RequesterKind requesterKind = StringUtils.hasText(requesterKindValue)
                ? RequesterKind.valueOf(requesterKindValue.trim().toUpperCase(Locale.ROOT))
                : properties.getRequesterKind();
        return new AuthzCraftRequester(requesterKind, requesterKey.trim());
    }

    private String resolveHeaderName(String configured, String fallback) {
        return StringUtils.hasText(configured) ? configured : fallback;
    }
}