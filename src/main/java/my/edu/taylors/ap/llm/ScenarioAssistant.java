package my.edu.taylors.ap.llm;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The conversational AI Service. You never implement this interface:
 * {@code AiServices.builder(ScenarioAssistant.class)...build()} creates a proxy that
 * <ol>
 *   <li>loads this user's memory,</li>
 *   <li>retrieves relevant document chunks (RAG),</li>
 *   <li>runs input guardrails,</li>
 *   <li>calls the model, executes any tools it requests, calls the model again ...</li>
 *   <li>runs output guardrails and returns the final text.</li>
 * </ol>
 * The system message is supplied per conversation by {@link AssistantFactory}.
 */
public interface ScenarioAssistant {

    @UserMessage("{{message}}")
    String chat(@MemoryId String userId, @V("message") String message);
}
