package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.RuleBlueprint;

public class RuleBlueprintResponse {
    private String id;
    private String tenantKey;
    private String blueprintKey;
    private String blueprintKind;
    private Integer blueprintVersion;
    private String displayName;
    private String description;
    private String predicateTemplate;
    private String inputSchema;
    private String lifecycleState;
    private String contentDigest;

    public static RuleBlueprintResponse from(RuleBlueprint blueprint) {
        RuleBlueprintResponse response = new RuleBlueprintResponse();
        response.setId(String.valueOf(blueprint.getId()));
        response.setTenantKey(blueprint.getTenantKey());
        response.setBlueprintKey(blueprint.getBlueprintKey());
        response.setBlueprintKind(blueprint.getBlueprintKind());
        response.setBlueprintVersion(blueprint.getBlueprintVersion());
        response.setDisplayName(blueprint.getDisplayName());
        response.setDescription(blueprint.getDescription());
        response.setPredicateTemplate(blueprint.getPredicateTemplate());
        response.setInputSchema(blueprint.getInputSchema());
        response.setLifecycleState(blueprint.getLifecycleState());
        response.setContentDigest(blueprint.getContentDigest());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantKey() { return tenantKey; }
    public void setTenantKey(String tenantKey) { this.tenantKey = tenantKey; }
    public String getBlueprintKey() { return blueprintKey; }
    public void setBlueprintKey(String blueprintKey) { this.blueprintKey = blueprintKey; }
    public String getBlueprintKind() { return blueprintKind; }
    public void setBlueprintKind(String blueprintKind) { this.blueprintKind = blueprintKind; }
    public Integer getBlueprintVersion() { return blueprintVersion; }
    public void setBlueprintVersion(Integer blueprintVersion) { this.blueprintVersion = blueprintVersion; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPredicateTemplate() { return predicateTemplate; }
    public void setPredicateTemplate(String predicateTemplate) { this.predicateTemplate = predicateTemplate; }
    public String getInputSchema() { return inputSchema; }
    public void setInputSchema(String inputSchema) { this.inputSchema = inputSchema; }
    public String getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(String lifecycleState) { this.lifecycleState = lifecycleState; }
    public String getContentDigest() { return contentDigest; }
    public void setContentDigest(String contentDigest) { this.contentDigest = contentDigest; }
}