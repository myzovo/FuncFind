package com.docvecrag.backend.service.session;

public final class SessionRuntimeConfigContextHolder {

    private static final ThreadLocal<SessionRuntimeConfig> HOLDER = new ThreadLocal<>();

    private SessionRuntimeConfigContextHolder() {
    }

    public static void setCurrent(SessionRuntimeConfig config) {
        HOLDER.set(config);
    }

    public static SessionRuntimeConfig getCurrent() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
