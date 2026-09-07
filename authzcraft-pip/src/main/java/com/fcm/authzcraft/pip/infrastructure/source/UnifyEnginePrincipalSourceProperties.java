package com.fcm.authzcraft.pip.infrastructure.source;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "authzcraft.pip.sync.unify-engine")
public class UnifyEnginePrincipalSourceProperties {

    private String sourceCode = "unify_engine";
    private String schemaName = "unify_engine";

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
}
