package com.fcm.authzcraft.demo.oa.interfaces.security;

import com.fcm.authzcraft.demo.oa.domain.model.CurrentUser;

public final class CurrentUserContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<CurrentUser>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) { HOLDER.set(currentUser); }
    public static CurrentUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}