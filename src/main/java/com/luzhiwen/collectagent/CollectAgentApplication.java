package com.luzhiwen.collectagent;

import com.luzhiwen.collectagent.shared.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 精简版智能催收 Agent 启动入口。
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(AgentProperties.class)
public class CollectAgentApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CollectAgentApplication.class, args);
    }
}

