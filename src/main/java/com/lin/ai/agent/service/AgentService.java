package com.lin.ai.agent.service;

import com.lin.ai.agent.model.dto.AgentParam;
import com.lin.ai.agent.model.vo.AgentReply;

/**
 * Agent 服务接口
 */
public interface AgentService {

    /**
     * 执行 Agent 任务
     */
    AgentReply execute(AgentParam param);
}
