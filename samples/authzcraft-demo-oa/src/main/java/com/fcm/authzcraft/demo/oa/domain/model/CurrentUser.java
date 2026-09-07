package com.fcm.authzcraft.demo.oa.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CurrentUser {
    private final String userKey;
    private final String displayName;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;
    private final List<RbacFieldGrant> fieldGrants;
    private final String requestKey;

    public CurrentUser(String userKey,
                       String displayName,
                       Set<String> roleCodes,
                       Set<String> permissionCodes,
                       List<RbacFieldGrant> fieldGrants,
                       String requestKey) {
        this.userKey = userKey;
        this.displayName = displayName;
        this.roleCodes = roleCodes == null ? Collections.<String>emptySet() : roleCodes;
        this.permissionCodes = permissionCodes == null ? Collections.<String>emptySet() : permissionCodes;
        this.fieldGrants = fieldGrants == null ? Collections.<RbacFieldGrant>emptyList() : fieldGrants;
        this.requestKey = requestKey;
    }

    public String getUserKey() { return userKey; }
    public String getDisplayName() { return displayName; }
    public Set<String> getRoleCodes() { return roleCodes; }
    public Set<String> getPermissionCodes() { return permissionCodes; }
    public List<RbacFieldGrant> getFieldGrants() { return fieldGrants; }
    public String getRequestKey() { return requestKey; }

    public boolean hasPermission(String permissionCode) {
        return permissionCodes.contains(permissionCode);
    }
}