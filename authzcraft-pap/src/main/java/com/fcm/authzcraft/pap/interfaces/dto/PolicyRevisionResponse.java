package com.fcm.authzcraft.pap.interfaces.dto;

import com.fcm.authzcraft.pap.domain.model.PolicyRevision;

public class PolicyRevisionResponse {
    private String id;
    private String policyId;
    private Integer revisionNo;
    private String blueprintId;
    private String predicateAst;
    private String argumentSchema;
    private String attributeReferences;
    private String contentDigest;
    private String revisionState;
    private String changeSummary;

    public static PolicyRevisionResponse from(PolicyRevision revision) {
        PolicyRevisionResponse response = new PolicyRevisionResponse();
        response.setId(String.valueOf(revision.getId()));
        response.setPolicyId(String.valueOf(revision.getPolicyId()));
        response.setRevisionNo(revision.getRevisionNo());
        response.setBlueprintId(revision.getBlueprintId() == null ? null : String.valueOf(revision.getBlueprintId()));
        response.setPredicateAst(revision.getPredicateAst());
        response.setArgumentSchema(revision.getArgumentSchema());
        response.setAttributeReferences(revision.getAttributeReferences());
        response.setContentDigest(revision.getContentDigest());
        response.setRevisionState(revision.getRevisionState());
        response.setChangeSummary(revision.getChangeSummary());
        return response;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
    public String getBlueprintId() { return blueprintId; }
    public void setBlueprintId(String blueprintId) { this.blueprintId = blueprintId; }
    public String getPredicateAst() { return predicateAst; }
    public void setPredicateAst(String predicateAst) { this.predicateAst = predicateAst; }
    public String getArgumentSchema() { return argumentSchema; }
    public void setArgumentSchema(String argumentSchema) { this.argumentSchema = argumentSchema; }
    public String getAttributeReferences() { return attributeReferences; }
    public void setAttributeReferences(String attributeReferences) { this.attributeReferences = attributeReferences; }
    public String getContentDigest() { return contentDigest; }
    public void setContentDigest(String contentDigest) { this.contentDigest = contentDigest; }
    public String getRevisionState() { return revisionState; }
    public void setRevisionState(String revisionState) { this.revisionState = revisionState; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
}