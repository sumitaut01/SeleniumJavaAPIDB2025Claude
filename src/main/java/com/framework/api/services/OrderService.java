package com.framework.api.services;

import com.framework.api.builder.ApiClient;
import com.framework.api.builder.RequestBuilder;
import com.framework.api.endpoints.ApiEndpoints;
import com.framework.api.validators.ResponseValidator;
import com.framework.context.TestContext;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * API Service for Order resource.
 * Demonstrates API→API chaining pattern.
 *
 * A typical chained flow:
 *   1. AuthService.loginExpectSuccess()      → stores token
 *   2. UserService.createUserExpectSuccess() → stores userId
 *   3. OrderService.createOrder()            → uses userId from context, stores orderId
 *   4. OrderService.getOrder()               → validates order was created correctly
 */
public class OrderService {

    private static final Logger log = LogManager.getLogger(OrderService.class);

    /**
     * Creates an order for the user currently in TestContext.
     * Requires userId to already be set in TestContext.
     */
    public Response createOrder(Map<String, Object> orderPayload) {
        String userId = TestContext.get().getUserId();
        if (userId == null) {
            throw new IllegalStateException("OrderService.createOrder: userId not in TestContext. "
                    + "Call UserService.createUserAndStoreContext() first.");
        }

        log.info("OrderService.createOrder → userId={}", userId);

        // Inject the userId from context into the payload
        orderPayload.put("userId", userId);

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .build();

        Response response = ApiClient.post(spec, ApiEndpoints.Order.CREATE, orderPayload);

        if (response.statusCode() == 201) {
            String orderId = response.jsonPath().getString("data.orderId");
            TestContext.get().setOrderId(orderId);
            log.info("OrderService: order created → orderId={}", orderId);
        }

        return response;
    }

    public Response getOrder(String orderId) {
        log.info("OrderService.getOrder → orderId={}", orderId);

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .build();

        return ApiClient.get(spec, ApiEndpoints.Order.GET_BY_ID,
                Map.of("orderId", orderId));
    }

    public Response updateOrderStatus(String orderId, String status) {
        log.info("OrderService.updateStatus → orderId={}, status={}", orderId, status);

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .withBody(Map.of("status", status))
                .build();

        return ApiClient.patch(spec, ApiEndpoints.Order.UPDATE_STATUS,
                Map.of("status", status),
                Map.of("orderId", orderId));
    }

    public Response getOrdersForContextUser() {
        String userId = TestContext.get().getUserId();

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .build();

        return ApiClient.get(spec, ApiEndpoints.Order.GET_BY_USER,
                Map.of("userId", userId));
    }
}
