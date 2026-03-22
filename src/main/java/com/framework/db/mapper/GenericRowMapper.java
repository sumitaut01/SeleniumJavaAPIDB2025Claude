package com.framework.db.mapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Built-in RowMapper that maps any ResultSet row to a Map<String, Object>.
 *
 * Useful when:
 *  - You don't want to create a POJO for a one-off query
 *  - You're doing exploratory / debug DB checks
 *  - You want column names to be dynamic
 *
 * Column names are lowercased for consistency.
 *
 * Usage:
 *   List<Map<String,Object>> rows = dbClient.query(
 *       "SELECT * FROM users WHERE email = ?",
 *       new GenericRowMapper(),
 *       email);
 *
 *   String status = (String) rows.get(0).get("status");
 */
public class GenericRowMapper implements RowMapper<Map<String, Object>> {

    @Override
    public Map<String, Object> map(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= colCount; i++) {
            String col   = meta.getColumnLabel(i).toLowerCase();
            Object value = rs.getObject(i);
            row.put(col, value);
        }
        return row;
    }
}
