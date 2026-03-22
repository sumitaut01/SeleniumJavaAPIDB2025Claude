package com.framework.api.endpoints;

/**
 * Central registry of all API endpoint paths.
 *
 * Rules:
 *  - Paths are relative (no base URL here — that comes from ConfigManager)
 *  - Use {placeholders} for path params → resolved by RestAssured pathParam()
 *  - Group by resource/domain
 *
 * Usage:
 *   given().pathParam("userId", 5)
 *          .get(ApiEndpoints.User.GET_BY_ID);
 */
public final class ApiEndpoints {

    private ApiEndpoints() {}

    // ── Auth ─────────────────────────────────────────────────────────────────
    public static final class Auth {
        public static final String LOGIN         = "/api/v1/auth/login";
        public static final String LOGOUT        = "/api/v1/auth/logout";
        public static final String REFRESH_TOKEN = "/api/v1/auth/refresh";
        public static final String REGISTER      = "/api/v1/auth/register";
    }

    // ── User ─────────────────────────────────────────────────────────────────
    public static final class User {
        public static final String CREATE        = "/api/v1/users";
        public static final String GET_ALL       = "/api/v1/users";
        public static final String GET_BY_ID     = "/api/v1/users/{userId}";
        public static final String UPDATE        = "/api/v1/users/{userId}";
        public static final String DELETE        = "/api/v1/users/{userId}";
        public static final String SEARCH        = "/api/v1/users/search";
    }

    // ── Order ─────────────────────────────────────────────────────────────────
    public static final class Order {
        public static final String CREATE        = "/api/v1/orders";
        public static final String GET_BY_ID     = "/api/v1/orders/{orderId}";
        public static final String GET_BY_USER   = "/api/v1/users/{userId}/orders";
        public static final String UPDATE_STATUS = "/api/v1/orders/{orderId}/status";
        public static final String CANCEL        = "/api/v1/orders/{orderId}/cancel";
    }

    // ── Product ───────────────────────────────────────────────────────────────
    public static final class Product {
        public static final String GET_ALL       = "/api/v1/products";
        public static final String GET_BY_ID     = "/api/v1/products/{productId}";
        public static final String CREATE        = "/api/v1/products";
        public static final String UPDATE        = "/api/v1/products/{productId}";
    }
}
