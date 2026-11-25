package my.edu.taylors.ap.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * OPTIONAL BONUS - Model Context Protocol (MCP) client.
 * <p>
 * Starts the reference MCP filesystem server as a child process (via npx, needs Node.js 20+)
 * and exposes its tools to the agent. The server is restricted to ONE folder,
 * {@code runtime/exports}, so the agent can write receipts/reports there but nowhere else.
 * Only the listed tools are exposed (least privilege).
 */
public final class McpSupport implements AutoCloseable {

    public static final List<String> ALLOWED_TOOLS = List.of("write_file", "read_text_file", "list_directory");

    private final McpClient client;
    private final McpToolProvider toolProvider;

    private McpSupport(McpClient client, McpToolProvider toolProvider) {
        this.client = client;
        this.toolProvider = toolProvider;
    }

    public static McpSupport filesystem(Path exportDir) throws IOException {
        Files.createDirectories(exportDir);
        String npx = System.getProperty("os.name").toLowerCase().contains("win") ? "npx.cmd" : "npx";
        StdioMcpTransport transport = StdioMcpTransport.builder()
                .command(List.of(npx, "-y", "@modelcontextprotocol/server-filesystem",
                        exportDir.toAbsolutePath().toString()))
                .logEvents(false)
                .build();
        McpClient client = DefaultMcpClient.builder()
                .key("files")
                .transport(transport)
                .toolExecutionTimeout(Duration.ofSeconds(30))
                .build();
        McpToolProvider provider = McpToolProvider.builder()
                .mcpClients(client)
                .filterToolNames(ALLOWED_TOOLS)
                .build();
        return new McpSupport(client, provider);
    }

    public McpToolProvider toolProvider() {
        return toolProvider;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}
