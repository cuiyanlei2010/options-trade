package me.dingtou.options.util;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import lombok.extern.slf4j.Slf4j;

/**
 * mcp工具类
 */
@Slf4j
public class McpUtils {

    /**
     * mcp客户端 {owner:{server:Client}}
     */
    private static final Map<String, Map<String, McpSyncClient>> OWNER_MCP_SERVERS = new ConcurrentHashMap<>();

    /**
     * 获取mcp客户端
     * 
     * @param owner
     * @return
     */
    public static Map<String, McpSyncClient> getOwnerMcpClient(String owner) {
        log.info("获取用户[{}]的Mcp客户端集合", owner);
        return OWNER_MCP_SERVERS.get(owner);
    }

    /**
     * 获取mcp客户端
     * 
     * @param owner      用户
     * @param serverName mcp服务名称
     * @return
     */
    public static McpSyncClient getMcpClient(String owner, String serverName) {
        log.info("获取用户[{}]的服务[{}]的Mcp客户端", owner, serverName);
        Map<String, McpSyncClient> mcpServers = OWNER_MCP_SERVERS.get(owner);
        if (null == mcpServers) {
            log.warn("用户[{}]未配置任何Mcp服务", owner);
            return null;
        }
        McpSyncClient mcpSyncClient = mcpServers.get(serverName);
        if (mcpSyncClient == null) {
            log.warn("用户[{}]的服务[{}]不存在", owner, serverName);
        }
        return mcpSyncClient;
    }

    /**
     * 初始化mcp客户端
     * 
     * @param owner       用户
     * @param mcpSettings mcp设置
     */
    public static void initMcpClient(String owner, String mcpSettings) {
        if (owner == null || owner.isEmpty()) {
            log.error("初始化McpClient失败: 用户名为空");
            return;
        }
        if (mcpSettings == null || mcpSettings.isEmpty()) {
            log.error("用户[{}]初始化McpClient失败: mcpSettings为空", owner);
            return;
        }
        
        log.info("开始初始化用户[{}]的Mcp客户端, 配置: {}", owner, mcpSettings);
        try {
            // 初始化mcp服务器
            JSONObject mcpServers = JSON.parseObject(mcpSettings).getJSONObject("mcpServers");
            if (null == mcpServers) {
                log.error("用户[{}]初始化McpClient失败: mcpServers配置为空", owner);
                return;
            }
            
            log.info("发现{}个Mcp服务需要初始化", mcpServers.size());
            for (String serverName : mcpServers.keySet()) {
                JSONObject server = mcpServers.getJSONObject(serverName);
                String url = server.getString("url");
                if (url == null || url.isEmpty()) {
                    log.error("服务[{}]初始化失败: URL为空", serverName);
                    continue;
                }
                Map<String, String> headers = server.getObject("headers", new TypeReference<Map<String, String>>() {});
                log.info("正在初始化Mcp服务: {} -> {}", serverName, url);
                initMcpSseClient(owner, serverName, url, headers);
            }
            log.info("用户[{}]的Mcp客户端初始化完成", owner);
        } catch (Exception e) {
            log.error("初始化McpClient时发生异常, 用户: " + owner, e);
        }
    }

    /**
     * 初始化mcp客户端
     * 
     * @param owner      用户
     * @param serverName mcp服务名称
     * @param url        mcp服务地址
     * @param headers    请求头
     * 
     * @return McpSyncClient
     */
    public synchronized static McpSyncClient initMcpSseClient(String owner,
            String serverName,
            String url,
            Map<String, String> headers) {

        Map<String, McpSyncClient> mcpServers = OWNER_MCP_SERVERS.get(owner);
        if (null == mcpServers) {
            mcpServers = new ConcurrentHashMap<>();
            OWNER_MCP_SERVERS.put(owner, mcpServers);
        }

        McpSyncClient mcpSyncClient = mcpServers.get(serverName);
        if (null != mcpSyncClient) {
            return mcpSyncClient;
        }

        // Create a transport
        Builder requestBuilder = HttpRequest.newBuilder()
                .header("Content-Type", "application/json");

        if (null != headers && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(url)
                .requestBuilder(requestBuilder)
                .build();
        // Create a sync client with custom configuration
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .capabilities(ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build())
                .build();
        // Initialize connection
        try {
            long startTime = System.currentTimeMillis();
            client.initialize();
            long cost = System.currentTimeMillis() - startTime;
            log.info("McpClient初始化成功, serverName: {}, 耗时: {}ms", serverName, cost);
        } catch (Exception e) {
            log.error("McpClient初始化失败, serverName: " + serverName, e);
            return null;
        }

        mcpServers.put(serverName, client);
        log.info("Mcp服务[{}]已注册到用户[{}]", serverName, owner);

        return client;
    }

}
