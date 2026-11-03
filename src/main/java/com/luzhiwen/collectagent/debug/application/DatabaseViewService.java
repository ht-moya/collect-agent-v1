package com.luzhiwen.collectagent.debug.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 数据表查看服务。
 */
@Service
public class DatabaseViewService {

    /** 允许查看的数据表。 */
    private static final List<String> TABLES = List.of(
            "event_layer",
            "agent_session_state",
            "agent_summary",
            "agent_long_term_memory",
            "agent_memory_pending",
            "agent_memory_extraction_checkpoint",
            "outbox_event",
            "dynamic_variable_definition",
            "dynamic_variable_value",
            "dynamic_variable_tool_config"
    );

    /** JDBC 查询模板。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建数据表查看服务。
     *
     * @param jdbcTemplate JDBC 查询模板
     */
    public DatabaseViewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取允许查看的数据表名。
     *
     * @return 数据表名列表
     */
    public List<String> listTables() {
        return TABLES;
    }

    /**
     * 查询数据表内容。
     *
     * @param table 数据表名
     * @param limit 最大返回行数
     * @return 表名、列名和行数据
     */
    public Map<String, Object> readTable(String table, int limit) {
        if (!TABLES.contains(table)) {
            throw new IllegalArgumentException("不允许查看该数据表：" + table);
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + " ORDER BY 1 DESC LIMIT " + safeLimit
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("limit", safeLimit);
        result.put("rows", rows);
        return result;
    }
}






