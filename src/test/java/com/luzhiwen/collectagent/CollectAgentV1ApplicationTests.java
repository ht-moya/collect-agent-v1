package com.luzhiwen.collectagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring 上下文测试。
 */
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "agent.embedding.enabled=false",
        "agent.llm.enabled=false"
})
class CollectAgentV1ApplicationTests {

    /**
     * 验证应用上下文可以正常启动。
     */
    @Test
    void contextLoads() {
    }
}





