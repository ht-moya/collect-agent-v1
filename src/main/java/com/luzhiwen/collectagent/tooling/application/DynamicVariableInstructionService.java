package com.luzhiwen.collectagent.tooling.application;

import com.luzhiwen.collectagent.tooling.infrastructure.persistence.entity.DynamicVariableDefinitionEntity;
import com.luzhiwen.collectagent.tooling.infrastructure.persistence.repository.DynamicVariableDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 动态变量提取指令构建服务。
 */
@Service
public class DynamicVariableInstructionService {

    /** 变量定义数据访问接口。 */
    private final DynamicVariableDefinitionRepository definitionRepository;

    /**
     * 创建动态变量指令服务。
     *
     * @param definitionRepository 变量定义数据访问接口
     */
    public DynamicVariableInstructionService(DynamicVariableDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    /**
     * 构建返回给 LLM 的动态变量提取说明。
     *
     * @return 动态变量提取说明
     */
    public String buildExtractionInstruction() {
        List<DynamicVariableDefinitionEntity> definitions =
                definitionRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
        if (definitions.isEmpty()) {
            return "请帮我提取一下变量的信息：暂无启用变量";
        }
        StringBuilder builder = new StringBuilder("请帮我提取一下变量的信息：");
        for (int index = 0; index < definitions.size(); index++) {
            if (index > 0) {
                builder.append("；");
            }
            DynamicVariableDefinitionEntity definition = definitions.get(index);
            builder.append(definition.getVariableKey())
                    .append(' ')
                    .append(definition.getDisplayName())
                    .append("（")
                    .append(definition.getDescription())
                    .append("）");
        }
        return builder.toString();
    }
}
