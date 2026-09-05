package com.apisentinel.gateway;

import java.util.Optional;

/**
 * ThreadLocal holder for the current request's ApiRequestContext.
 * Enables controllers, security filters, and audit loggers to safely access
 * the active context.
 */
public final class ApiRequestContextHolder {

    private static final ThreadLocal<ApiRequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private ApiRequestContextHolder() {
    }

    public static void setContext(ApiRequestContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static Optional<ApiRequestContext> getContext() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
