package com.campushub.utils;

public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> CURRENT_USER_ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setCurrentUserRole(Integer role) {
        CURRENT_USER_ROLE.set(role);
    }

    public static Integer getCurrentUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
