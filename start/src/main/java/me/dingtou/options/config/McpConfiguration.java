package me.dingtou.options.config;

import me.dingtou.options.service.mcp.DataQueryMcpService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfiguration {
	@Bean
	public ToolCallbackProvider dataQueryTools(DataQueryMcpService dataQueryMcpService) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(dataQueryMcpService)
				.build();
	}

}
