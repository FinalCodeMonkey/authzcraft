package com.fcm.authzcraft.pap.domain.model;

public class PolicyRevision {
    private Long id;
    private Long policyId;
    private Integer revisionNo;
    private Long blueprintId;
    private String predicateAst;
    private String argumentSchema;
    private String attributeReferences;
    private String contentDigest;
    private String revisionState;
    private String changeSummary;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
    public Long getBlueprintId() { return blueprintId; }
    public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
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