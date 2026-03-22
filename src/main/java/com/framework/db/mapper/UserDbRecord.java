package com.framework.db.mapper;

import lombok.Builder;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * DB record POJO for the 'users' table.
 * Maps columns returned by DbQueries.User.FIND_BY_ID and FIND_BY_EMAIL.
 *
 * Also serves as its own RowMapper via the static factory method.
 */
@Data
@Builder
public class UserDbRecord {

    private String    userId;
    private String    email;
    private String    firstName;
    private String    lastName;
    private String    status;
    private String    country;
    private String    role;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Returns a RowMapper<UserDbRecord> for use with DbClient.query().
     *
     * Usage:
     *   List<UserDbRecord> users = pgDb.query(
     *       DbQueries.User.FIND_BY_ID,
     *       UserDbRecord.rowMapper(),
     *       userId);
     */
    public static RowMapper<UserDbRecord> rowMapper() {
        return rs -> UserDbRecord.builder()
                .userId(rs.getString("user_id"))
                .email(rs.getString("email"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .status(rs.getString("status"))
                .country(rs.getString("country"))
                .role(rs.getString("role"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }
}
