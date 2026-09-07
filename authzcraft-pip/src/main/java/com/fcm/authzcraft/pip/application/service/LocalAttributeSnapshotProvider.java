package com.fcm.authzcraft.pip.application.service;

import com.fcm.authzcraft.api.attribute.AttributeFailure;
import com.fcm.authzcraft.api.attribute.AttributeReference;
import com.fcm.authzcraft.api.attribute.AttributeSnapshot;
import com.fcm.authzcraft.api.attribute.AttributeSnapshotRequest;
import com.fcm.authzcraft.api.attribute.AttributeValue;
import com.fcm.authzcraft.api.attribute.ResolveKind;
import com.fcm.authzcraft.api.common.RequesterKind;
import com.fcm.authzcraft.api.common.ValueKind;
import com.fcm.authzcraft.api.plan.FailureCode;
import com.fcm.authzcraft.api.service.AttributeSnapshotProvider;
import com.fcm.authzcraft.pip.domain.model.PrincipalAttributeProjection;
import com.fcm.authzcraft.pip.domain.repository.PrincipalAttributeRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocalAttributeSnapshotProvider implements AttributeSnapshotProvider {
    private static final String TRUST_LEVEL_LOCAL_SYNC = "LOCAL_SYNC";

    private final PrincipalAttributeRepository repository;

    public LocalAttributeSnapshotProvider(PrincipalAttributeRepository repository) {
        this.repository = repository;
    }

    @Override
    public AttributeSnapshot resolve(AttributeSnapshotRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("attribute snapshot request must not be null");
        }
        LocalDateTime decisionTime = LocalDateTime.now();
        AttributeSnapshot snapshot = new AttributeSnapshot();
        Map<String, AttributeValue> attributes = new LinkedHashMap<String, AttributeValue>();
        PrincipalAttributeProjection user = resolveUser(request);
        for (AttributeReference reference : references(request)) {
            if (reference == null || reference.getResolveKind() == null || !StringUtils.hasText(reference.getAttributeKey())) {
                continue;
            }
            attributes.put(reference.getAttributeKey(), resolveAttribute(request, reference, user, decisionTime));
        }
        snapshot.setAttributes(attributes);
        snapshot.setTrustLevel(TRUST_LEVEL_LOCAL_SYNC);
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setExpiresAt(snapshot.getGeneratedAt().plusSeconds(60));
        snapshot.setAttributeDigest(sha256(canonical(attributes)));
        snapshot.setSourceVersion("principal-projection:" + snapshot.getAttributeDigest());
        return snapshot;
    }

    private PrincipalAttributeProjection resolveUser(AttributeSnapshotRequest request) {
        if (!RequesterKind.USER.equals(request.getRequesterKind())) {
            return null;
        }
        return repository.findActiveUser(request.getTenantKey(), request.getRequesterKey());
    }

    private AttributeValue resolveAttribute(AttributeSnapshotRequest request, AttributeReference reference,
                                            PrincipalAttributeProjection user, LocalDateTime decisionTime) {
        if (user == null) {
            return unavailable(reference, FailureCode.SUBJECT_UNAVAILABLE, "requester user is unavailable");
        }
        ResolveKind resolveKind = reference.getResolveKind();
        if (ResolveKind.REQUESTER_USER_ID.equals(resolveKind)) {
            return available(reference, StringUtils.hasText(user.getUserId()) ? user.getUserId() : request.getRequesterKey());
        }
        if (ResolveKind.REQUESTER_DEPARTMENT_CODE.equals(resolveKind)) {
            return textAttribute(reference, user.getDepartmentCode());
        }
        if (ResolveKind.REQUESTER_POSITION_CODE.equals(resolveKind)) {
            return textAttribute(reference, user.getPostCode());
        }
        if (ResolveKind.MANAGED_DEPARTMENT_CODES.equals(resolveKind)) {
            return available(reference, repository.findManagedDepartmentCodes(request.getTenantKey(), request.getRequesterKey(),
                    stringList(request.getGrantArguments() == null ? null : request.getGrantArguments().get("departmentScopes")),
                    text(request.getGrantArguments() == null ? null : request.getGrantArguments().get("subDepartmentDepth"))));
        }
        if (ResolveKind.ROLE_KEYS.equals(resolveKind)) {
            return available(reference, repository.findRoleKeys(request.getTenantKey(), request.getAppKey(), user.getPrincipalId(), decisionTime));
        }
        if (ResolveKind.GROUP_KEYS.equals(resolveKind)) {
            return available(reference, repository.findGroupKeys(request.getTenantKey(), request.getAppKey(), user.getPrincipalId(), decisionTime));
        }
        return unavailable(reference, FailureCode.ATTRIBUTE_MISSING, "unsupported resolveKind: " + resolveKind);
    }

    private AttributeValue textAttribute(AttributeReference reference, String value) {
        if (!StringUtils.hasText(value)) {
            return unavailable(reference, FailureCode.ATTRIBUTE_MISSING, "attribute is missing: " + reference.getAttributeKey());
        }
        return available(reference, value);
    }

    private AttributeValue available(AttributeReference reference, Object value) {
        AttributeValue attributeValue = new AttributeValue(reference.getAttributeKey(), valueKind(reference), value);
        attributeValue.setAvailable(true);
        return attributeValue;
    }

    private AttributeValue unavailable(AttributeReference reference, FailureCode failureCode, String message) {
        AttributeValue attributeValue = new AttributeValue(reference.getAttributeKey(), valueKind(reference), null);
        AttributeFailure failure = new AttributeFailure();
        failure.setFailureCode(failureCode);
        failure.setAttributeKey(reference.getAttributeKey());
        failure.setResolveKind(reference.getResolveKind());
        failure.setRetryable(false);
        failure.setMessage(message);
        attributeValue.setAvailable(false);
        attributeValue.setFailure(failure);
        return attributeValue;
    }

    private ValueKind valueKind(AttributeReference reference) {
        return reference.getValueKind() == null ? ValueKind.STRING : reference.getValueKind();
    }

    private List<AttributeReference> references(AttributeSnapshotRequest request) {
        if (request.getAttributeReferences() == null) {
            return Collections.emptyList();
        }
        return request.getAttributeReferences();
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        if (value instanceof Iterable) {
            for (Object item : (Iterable<Object>) value) {
                String text = text(item);
                if (StringUtils.hasText(text)) {
                    result.add(text.trim());
                }
            }
            return result;
        }
        String text = text(value);
        if (StringUtils.hasText(text)) {
            result.add(text.trim());
        }
        return result;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String canonical(Map<String, AttributeValue> attributes) {
        List<String> keys = new ArrayList<String>(attributes.keySet());
        Collections.sort(keys);
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            AttributeValue value = attributes.get(key);
            builder.append(key).append('=').append(value.isAvailable()).append(':').append(String.valueOf(value.getValue())).append('|');
        }
        return builder.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to calculate attribute digest", exception);
        }
    }
}