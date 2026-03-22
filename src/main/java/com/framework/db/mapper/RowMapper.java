package com.framework.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a single ResultSet row to a typed object T.
 *
 * Implement one RowMapper per query / POJO type.
 * The mapper is called once per row by DbClient.
 *
 * Example:
 *   public class UserRowMapper implements RowMapper<UserDbRecord> {
 *       public UserDbRecord map(ResultSet rs) throws SQLException {
 *           return UserDbRecord.builder()
 *               .userId(rs.getString("user_id"))
 *               .email(rs.getString("email"))
 *               .status(rs.getString("status"))
 *               .build();
 *       }
 *   }
 *
 * @param <T> the POJO type this mapper produces
 */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
