package com.fcm.authzcraft.pap.application.command;

public class CreatePolicyRevisionCommand {
    private String blueprintId;
    private String templateBinding;
    private String customAst;
    private String argumentSchema;
    private String attributeReferences;
    private String changeSummary;

    public String getBlueprintId() { return blueprintId; }
    public void setBlueprintId(String blueprintId) { this.blueprintId = blueprintId; }
    public String getTemplateBinding() { return templateBinding; }
    public void setTemplateBinding(String templateBinding) { this.templateBinding = templateBinding; }
    public String getCustomAst() { return customAst; }
    public void setCustomAst(String customAst) { this.customAst = customAst; }
    public String getArgumentSchema() { return argumentSchema; }
    public void setArgumentSchema(String argumentSchema) { this.argumentSchema = argumentSchema; }
    public String getAttributeReferences() { return attributeReferences; }
    public void setAttributeReferences(String attributeReferences) { this.attributeReferences = attributeReferences; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
}