package com.fcm.authzcraft.pdp.domain.model;

public class PublishedPolicySnapshot {
    private Long policyId;
    private String effectKind;
    private Long revisionId;
    private String predicateAst;
    private String argumentSchema;
    private String attributeReferences;
    private String contentDigest;

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getEffectKind() { return effectKind; }
    public void setEffectKind(String effectKind) { this.effectKind = effectKind; }
    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }
    public String getPredicateAst() { return predicateAst; }
    public void setPredicateAst(String predicateAst) { this.predicateAst = predicateAst; }
    public String getArgumentSchema() { return argumentSchema; }
    public void setArgumentSchema(String argumentSchema) { this.argumentSchema = argumentSchema; }
    public String getAttributeReferences() { return attributeReferences; }
    public void setAttributeReferences(String attributeReferences) { this.attributeReferences = attributeReferences; }
    public String getContentDigest() { return contentDigest; }
    public void setContentDigest(String contentDigest) { this.contentDigest = contentDigest; }
}