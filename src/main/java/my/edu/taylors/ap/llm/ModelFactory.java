package my.edu.taylors.ap.llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import my.edu.taylors.ap.config.AppConfig;

import java.util.List;

/**
 * Creates the {@link ChatModel} named in config.properties. Every other class depends only on
 * the {@code ChatModel} interface, so switching provider is a one-line config change
 * (dependency inversion without a framework).
 */
public final class ModelFactory {

    private ModelFactory() { }

    public static ChatModel create(AppConfig cfg, List<ChatModelListener> listeners) {
        return switch (cfg.provider()) {
            case OLLAMA -> OllamaChatModel.builder()
                    .baseUrl(or(cfg.baseUrl(), "http://localhost:11434"))
                    .modelName(cfg.modelName())
                    .temperature(cfg.temperature())
                    .timeout(cfg.timeout())
                    .think(false)               // reasoning models (qwen3): faster, cleaner tool calls
                    .listeners(listeners)
                    .build();
            case LMSTUDIO -> OpenAiChatModel.builder()   // LM Studio speaks the OpenAI protocol
                    .baseUrl(or(cfg.baseUrl(), "http://localhost:1234/v1"))
                    .apiKey("lm-studio")
                    .modelName(cfg.modelName())
                    .temperature(cfg.temperature())
                    .timeout(cfg.timeout())
                    .listeners(listeners)
                    .build();
            case OPENAI -> OpenAiChatModel.builder()
                    .apiKey(cfg.apiKey())
                    .modelName(cfg.modelName())
                    .timeout(cfg.timeout())
                    .listeners(listeners)
                    .build();
            case ANTHROPIC -> AnthropicChatModel.builder()
                    .apiKey(cfg.apiKey())
                    .modelName(cfg.modelName())
                    .temperature(cfg.temperature())
                    .maxTokens(1024)
                    .timeout(cfg.timeout())
                    .listeners(listeners)
                    .build();
        };
    }

    private static String or(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
