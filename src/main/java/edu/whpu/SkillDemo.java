package edu.whpu;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.harness.agent.HarnessAgent;

import java.io.IOException;
import java.nio.file.Path;

public class SkillDemo {
    // 应用启动时配置一次，之后只调用 build()。
    private static final HarnessAgent.Builder AGENT_BUILDER;

    static {
        try {
            AGENT_BUILDER = HarnessAgent.builder()
                    .name("demo-agent")
                    .agentId("demo-agent")
                    .model("dashscope:qwen3.8-max-0902")
                    .workspace(Path.of(".agentscope/workspace"))
                    .skillRepository(new ClasspathSkillRepository("skills"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try (HarnessAgent agent = AGENT_BUILDER.build()) {
            RuntimeContext ctx = RuntimeContext.builder()
                    .userId("wenbin").sessionId("session-001").build();
            Msg msg = agent.call(new UserMessage("帮我生成一个pdf文件，内容为：demo test for pdf"), ctx).block();
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
