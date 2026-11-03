package com.luzhiwen.collectagent;

import com.luzhiwen.collectagent.agentruntime.application.FinalAnswerEnvelopeService;
import com.luzhiwen.collectagent.agentruntime.domain.FinalAnswerPayload;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionRequest;
import com.luzhiwen.collectagent.conversation.api.dto.DynamicVariableDefinitionResponse;
import com.luzhiwen.collectagent.tooling.application.DynamicVariableService;
import com.luzhiwen.collectagent.tooling.application.ToolRegistry;
import com.luzhiwen.collectagent.tooling.domain.ToolResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态变量定义、无参数工具说明和变量值持久化集成测试。
 */
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "agent.embedding.enabled=false",
        "agent.llm.enabled=false",
        "agent.outbox.relay-interval-ms=100000"
})
class DynamicVariableServiceIntegrationTest {

    /** 动态变量服务。 */
    @Autowired
    private DynamicVariableService dynamicVariableService;

    /** 工具注册中心。 */
    @Autowired
    private ToolRegistry toolRegistry;

    /** 最终 JSON 信封解析服务。 */
    @Autowired
    private FinalAnswerEnvelopeService finalAnswerEnvelopeService;

    /**
     * 验证无参数工具返回 MySQL 中的变量说明，并能够保存会话变量值。
     */
    @Test
    void shouldProvideDynamicVariableInstructionAndPersistValue() {
        DynamicVariableDefinitionRequest request = new DynamicVariableDefinitionRequest();
        request.setVariableKey("address");
        request.setDisplayName("地址");
        request.setDescription("用户当前居住地址");
        request.setExtractionHint("只提取用户明确说出的完整地址");
        request.setEnabled(true);
        request.setSortOrder(1);

        DynamicVariableDefinitionResponse definition = dynamicVariableService.createDefinition(request);

        assertEquals("address", definition.getVariableKey());
        assertTrue(dynamicVariableService.shouldExtract());
        var dynamicTool = toolRegistry.getRequiredTool(DynamicVariableService.TOOL_NAME);
        assertEquals("", dynamicTool.description());
        assertEquals(Map.of(), dynamicTool.parameters().get("properties"));

        ToolResult toolResult = dynamicTool.execute(Map.of());
        assertTrue(toolResult.isSuccess());
        assertTrue(toolResult.getMessage().contains("address 地址"));
        assertTrue(toolResult.getMessage().contains("用户当前居住地址"));

        FinalAnswerPayload payload = finalAnswerEnvelopeService.parseAndPersist(
                "trace-dynamic-001",
                "S-DYNAMIC-001",
                "C10001",
                10L,
                "{\"variableMap\":{\"address\":\"浙江省杭州市西湖区\"},\"answer\":\"业务回复\"}"
        );

        assertEquals("业务回复", payload.getAnswer());
        assertEquals("浙江省杭州市西湖区", payload.getVariableMap().get("address"));
        assertEquals("浙江省杭州市西湖区",
                dynamicVariableService.listValues("S-DYNAMIC-001").get(0).getVariableValue());
    }
}






