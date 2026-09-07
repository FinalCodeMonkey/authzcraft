package com.fcm.authzcraft.pap.application.command;

public class UpdateAccessGrantCommand {
    private String validFrom;
    private String validUntil;
    private String grantSource;
    private String reason;

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }
    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
    public String getGrantSource() { return grantSource; }
    public void setGrantSource(String grantSource) { this.grantSource = grantSource; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}