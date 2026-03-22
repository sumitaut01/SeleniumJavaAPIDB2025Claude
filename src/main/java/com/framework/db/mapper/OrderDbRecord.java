package com.framework.db.mapper;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DB record POJO for the 'orders' table.
 * Maps columns returned by DbQueries.Order queries.
 */
@Data
@Builder
public class OrderDbRecord {

    private String     orderId;
    private String     userId;
    private String     productId;
    private int        quantity;
    private String     status;
    private String     currency;
    private BigDecimal totalAmount;
    private Timestamp  createdAt;

    /** RowMapper factory — pass to DbClient.query() or DbClient.queryOne(). */
    public static RowMapper<OrderDbRecord> rowMapper() {
        return rs -> OrderDbRecord.builder()
                .orderId(rs.getString("order_id"))
                .userId(rs.getString("user_id"))
                .productId(rs.getString("product_id"))
                .quantity(rs.getInt("quantity"))
                .status(rs.getString("status"))
                .currency(rs.getString("currency"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}
