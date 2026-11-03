package com.luzhiwen.collectagent.debug.api;

import com.luzhiwen.collectagent.debug.application.DatabaseViewService;
import com.luzhiwen.collectagent.debug.application.VariableCacheBenchmarkService;
import com.luzhiwen.collectagent.debug.api.dto.VariableCacheBenchmarkRequest;
import com.luzhiwen.collectagent.debug.api.dto.VariableCacheBenchmarkResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据库表查看接口。
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    /** 数据表查看服务。 */
    private final DatabaseViewService databaseViewService;

    /** 动态变量缓存基准测试服务。 */
    private final VariableCacheBenchmarkService variableCacheBenchmarkService;

    /**
     * 创建调试控制器。
     *
     * @param databaseViewService 数据表查看服务
     * @param variableCacheBenchmarkService 动态变量缓存基准测试服务
     */
    public DebugController(
            DatabaseViewService databaseViewService,
            VariableCacheBenchmarkService variableCacheBenchmarkService
    ) {
        this.databaseViewService = databaseViewService;
        this.variableCacheBenchmarkService = variableCacheBenchmarkService;
    }

    /**
     * 查询允许查看的数据表。
     *
     * @return 数据表名称列表
     */
    @GetMapping("/tables")
    public List<String> tables() {
        return databaseViewService.listTables();
    }

    /**
     * 执行动态变量缓存命中 A/B 测试。
     *
     * @param request 基准测试请求
     * @return 基准测试结果
     */
    @PostMapping("/variable-cache-benchmark")
    public VariableCacheBenchmarkResponse variableCacheBenchmark(
            @RequestBody VariableCacheBenchmarkRequest request
    ) {
        return variableCacheBenchmarkService.run(request);
    }

    /**
     * 查询指定数据表内容。
     *
     * @param table 数据表名
     * @param limit 最大行数
     * @return 表数据
     */
    @GetMapping("/tables/{table}")
    public Map<String, Object> table(
            @PathVariable String table,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return databaseViewService.readTable(table, limit);
    }
}

