package com.luzhiwen.collectagent.conversation.api;

import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionResponse;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableToolConfigRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableToolConfigResponse;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableValueResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 动态变量配置和会话变量值接口。
 */
@RestController
@RequestMapping("/api/dynamic-variables")
public class DynamicVariableController {

    /** 动态变量服务。 */
    private final DynamicVariableService dynamicVariableService;

    /**
     * 创建动态变量控制器。
     *
     * @param dynamicVariableService 动态变量服务
     */
    public DynamicVariableController(DynamicVariableService dynamicVariableService) {
        this.dynamicVariableService = dynamicVariableService;
    }

    /**
     * 查询动态变量抽取工具配置。
     *
     * @return 工具配置
     */
    @GetMapping("/tool-config")
    public DynamicVariableToolConfigResponse toolConfig() {
        return dynamicVariableService.getToolConfigResponse();
    }

    /**
     * 更新动态变量抽取工具配置。
     *
     * @param request 工具配置请求
     * @return 更新后的工具配置
     */
    @PutMapping("/tool-config")
    public DynamicVariableToolConfigResponse updateToolConfig(
            @RequestBody DynamicVariableToolConfigRequest request
    ) {
        return dynamicVariableService.updateToolConfig(request);
    }

    /**
     * 查询全部动态变量定义。
     *
     * @return 动态变量定义列表
     */
    @GetMapping("/definitions")
    public List<DynamicVariableDefinitionResponse> definitions() {
        return dynamicVariableService.listDefinitions();
    }

    /**
     * 创建动态变量定义。
     *
     * @param request 变量定义请求
     * @return 创建后的变量定义
     */
    @PostMapping("/definitions")
    public DynamicVariableDefinitionResponse createDefinition(
            @RequestBody DynamicVariableDefinitionRequest request
    ) {
        return dynamicVariableService.createDefinition(request);
    }

    /**
     * 更新动态变量定义。
     *
     * @param id 变量定义主键
     * @param request 变量定义请求
     * @return 更新后的变量定义
     */
    @PutMapping("/definitions/{id}")
    public DynamicVariableDefinitionResponse updateDefinition(
            @PathVariable Long id,
            @RequestBody DynamicVariableDefinitionRequest request
    ) {
        return dynamicVariableService.updateDefinition(id, request);
    }

    /**
     * 删除动态变量定义。
     *
     * @param id 变量定义主键
     */
    @DeleteMapping("/definitions/{id}")
    public void deleteDefinition(@PathVariable Long id) {
        dynamicVariableService.deleteDefinition(id);
    }

    /**
     * 查询指定会话的动态变量值。
     *
     * @param sessionId 会话唯一标识
     * @return 动态变量值列表
     */
    @GetMapping("/values")
    public List<DynamicVariableValueResponse> values(@RequestParam String sessionId) {
        return dynamicVariableService.listValues(sessionId);
    }
}
