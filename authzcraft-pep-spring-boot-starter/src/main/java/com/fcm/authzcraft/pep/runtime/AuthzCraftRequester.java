package com.fcm.authzcraft.pep.runtime;

import com.fcm.authzcraft.api.common.RequesterKind;

public class AuthzCraftRequester {
    private final RequesterKind requesterKind;
    private final String requesterKey;

    public AuthzCraftRequester(RequesterKind requesterKind, String requesterKey) {
        this.requesterKind = requesterKind;
        this.requesterKey = requesterKey;
    }

    public RequesterKind getRequesterKind() {
        return requesterKind;
    }

    public String getRequesterKey() {
        return requesterKey;
    }
}