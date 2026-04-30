package com.lin.ai.agent.service;

import java.util.List;

/**
 * 任务规划服务 - 将复杂任务拆解为有序的子步骤
 */
public interface PlanningService {

    /**
     * 分解任务为具体的执行步骤
     *
     * @param taskDescription 用户任务描述
     * @param modelId         模型 ID，为空则使用默认模型
     * @return 有序的子步骤列表
     */
    List<String> decomposeTask(String taskDescription, String modelId);
}
