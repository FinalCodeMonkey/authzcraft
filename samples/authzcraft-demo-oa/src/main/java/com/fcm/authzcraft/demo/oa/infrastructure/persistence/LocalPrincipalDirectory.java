package com.fcm.authzcraft.demo.oa.infrastructure.persistence;

import com.fcm.authzcraft.demo.oa.domain.gateway.PrincipalDirectory;
import com.fcm.authzcraft.demo.oa.domain.repository.RbacRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "authzcraft.demo-oa.rbac", name = "storage-mode", havingValue = "LOCAL")
public class LocalPrincipalDirectory implements PrincipalDirectory {

    private final RbacRepository repository;

    public LocalPrincipalDirectory(RbacRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<String, Object> findActiveUserProjection(String userKey) {
        return repository.findActiveUser(userKey);
    }

    @Override
    public Set<String> listRoleCodes(String userKey) {
        return repository.listRoleCodes(userKey);
    }

    @Override
    public List<Map<String, Object>> listRoles() {
        return repository.listRoles();
    }

    @Override
    public List<Map<String, Object>> listGroups() {
        return repository.listGroups();
    }

    @Override
    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> users = repository.listUsersWithAssociations();
        for (Map<String, Object> user : users) {
            user.put("roles", splitComma(text(user.get("roleCodes"))));
            user.put("permissions", splitComma(text(user.get("permissionCodes"))));
            user.remove("roleCodes");
            user.remove("permissionCodes");
        }
        return users;
    }

    @Override
    public List<Map<String, Object>> listRoleUsers(String roleId) {
        return repository.listRoleUsers(localRoleId(roleId));
    }

    @Override
    public List<Map<String, Object>> listGroupUsers(String groupId) {
        return repository.listGroupUsers(localGroupId(groupId));
    }

    @Override
    public List<Map<String, Object>> listUserMemberships(String userKey) {
        return repository.listUserMemberships(userKey);
    }

    @Override
    public int countRoleUsers(String roleId) {
        return repository.countRoleUsers(localRoleId(roleId));
    }

    @Override
    public int countGroupUsers(String groupId) {
        return repository.countGroupUsers(localGroupId(groupId));
    }

    @Override
    public String roleCode(String roleId) {
        return repository.findRoleCodeById(localRoleId(roleId));
    }

    @Override
    public String groupCode(String groupId) {
        return groupId;
    }

    @Override
    public boolean supportsDataPermissionTemplates() {
        return false;
    }

    private Long localRoleId(String roleId) {
        return Long.valueOf(roleId);
    }

    private Long localGroupId(String groupId) {
        return Long.valueOf(groupId);
    }

    private List<String> splitComma(String value) {
        List<String> result = new ArrayList<String>();
        if (StringUtils.hasText(value)) {
            for (String part : value.split(",")) {
                if (StringUtils.hasText(part)) {
                    result.add(part.trim());
                }
            }
        }
        return result;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}