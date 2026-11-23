package edu.whpu;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.tools.McpServerConfig;
import io.agentscope.harness.agent.tools.ToolsConfig;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AgentScope 2.x MCP 示例：
 * 1. 直连 MCP server，打印它暴露的工具；
 * 2. 把 MCP server 注册进 HarnessAgent，让模型自动调用这些工具。
 *
 * 运行前确保 MCP server 已启动：http://localhost:8080/mcp
 */
public class McpDemo {

    private static final String MCP_URL = "http://localhost:8080/mcp";
    private static final String SERVER_NAME = "weather-mcp-server";
    private static final String MODEL = "dashscope:qwen3.8-max-0902";

    public static void main(String[] args) {
        listTools();

        ToolsConfig toolsConfig = new ToolsConfig();
        // 保留 Agent 内置工具，避免 toolsConfig 只放行 MCP 工具
        toolsConfig.setDefaultToolsEnabled(true);

        McpServerConfig mcp = new McpServerConfig();
        // /mcp 这种 HTTP 端点用 streamable-http；如果 server 是 SSE 端点则改成 "sse"
        mcp.setTransport("streamable-http");
        mcp.setUrl(MCP_URL);
        // 默认是 false：不显式打开的话，该 server 的工具一个都不会注册
        mcp.setDefaultToolsEnabled(true);
        mcp.setTimeout(Duration.ofSeconds(30));
        // 连不上时直接在 build() 抛错，方便定位问题
        mcp.setRequired(true);
        toolsConfig.setMcpServers(Map.of(SERVER_NAME, mcp));

        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("mcp-demo-agent")
                .agentId("mcp-demo-agent")
                .sysPrompt("你是助手。需要时请调用 MCP 工具完成任务。")
                .model(MODEL)
                .workspace(Path.of(".agentscope/workspace"))
                .toolsConfig(toolsConfig)
                // MCP 工具默认被视为“非只读”，会走 ask 确认流程并挂起本轮；
                // demo 里用 BYPASS 免确认，让工具直接被调用
                .permissionContext(PermissionContextState.builder()
                        .mode(PermissionMode.BYPASS)
                        .build());

        try (HarnessAgent agent = builder.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .userId("wenbin").sessionId("mcp-demo-session").build();
            Msg reply = agent.call(new UserMessage("帮我查一下北京今天的天气，告诉我查询结果。"), ctx).block();
            System.out.println("=== Agent 回复 ===");
            System.out.println(reply.getTextContent());
        }
    }

    /** 直连 MCP server，确认连通性并查看工具清单。 */
    private static void listTools() {
        try (McpClientWrapper client = McpClientBuilder.create(SERVER_NAME)
                .streamableHttpTransport(MCP_URL)
                .timeout(Duration.ofSeconds(30))
                .buildSync()) {
            client.initialize().block();
            List<McpSchema.Tool> tools = client.listTools().block();

            System.out.println("=== MCP server 工具列表 ===");
            if (tools == null || tools.isEmpty()) {
                System.out.println("(无工具)");
                return;
            }
            for (McpSchema.Tool tool : tools) {
                System.out.printf("- %s: %s%n", tool.name(), tool.description());
            }
        } catch (Exception e) {
            System.err.println("连接 MCP server 失败: " + e.getMessage());
        }
    }
}
