package com.arenahub.security;

public final class AuthContext {
    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static CurrentUser get() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw new IllegalStateException("当前请求未认证");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
