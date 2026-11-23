package edu.whpu;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;

import java.nio.file.Path;

public class AgentDemo {
    // 应用启动时配置一次，之后只调用 build()。
    private static final HarnessAgent.Builder AGENT_BUILDER = HarnessAgent.builder()
            .name("jiagoushi")
            .agentId("jiagoushi")
            .sysPrompt("你是一个软件架构师")
            .model("dashscope:qwen3.8-flash")
            .workspace(Path.of(".agentscope/workspace"));

    public static void main(String[] args) {
        try (HarnessAgent agent = AGENT_BUILDER.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .userId("wenbin").sessionId("session-001").build();
            Msg msg = agent.call(new UserMessage("我叫wenbin"), ctx).block();
            System.out.println(msg.getTextContent());

        }

        // 下一次请求使用新实例；相同会话身份从持久日志恢复上下文。
//        try (HarnessAgent agent = AGENT_BUILDER.build()) {
//            RuntimeContext ctx = RuntimeContext.builder()
//                    .userId("alice").sessionId("demo-session").build();
//            Msg reply = agent.call(new UserMessage("帮我做一个关于Higress的技术分享"), ctx).block();
//            System.out.println(reply.getTextContent());
//        }
    }
}
