// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import my.edu.taylors.ap.memory.FileChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** A6: per-user memory survives a "restart", user ids cannot escape the folder, corrupt files are quarantined. */
class FileChatMemoryStoreTest {

    static final String QUILLMARK = "generated";

    @TempDir
    Path dir;

    @Test
    void conversationSurvivesRestartAndUsersAreSeparate() {
        FileChatMemoryStore first = new FileChatMemoryStore(dir);
        first.updateMessages("STU-145", List.of(UserMessage.from("hi"), AiMessage.from("hello")));
        first.updateMessages("STU-201", List.of(UserMessage.from("other user")));

        FileChatMemoryStore afterRestart = new FileChatMemoryStore(dir);   // new object = app restarted
        List<ChatMessage> restored = afterRestart.getMessages("STU-145");
        assertEquals(2, restored.size());
        assertEquals("hi", ((UserMessage) restored.get(0)).singleText());
        assertEquals(1, afterRestart.getMessages("STU-201").size());
        assertTrue(afterRestart.getMessages("nobody").isEmpty());
    }

    @Test
    void pathTraversalStaysInsideTheMemoryFolder() throws Exception {
        FileChatMemoryStore store = new FileChatMemoryStore(dir.resolve("memory"));
        store.updateMessages("../../evil", List.of(UserMessage.from("x")));
        assertFalse(Files.exists(dir.resolve("evil.json")));
        try (Stream<Path> files = Files.list(dir.resolve("memory"))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void corruptedFileIsQuarantinedAndConversationRestarts() throws Exception {
        Files.writeString(dir.resolve("STU-145.json"), "{ this is not json");
        FileChatMemoryStore store = new FileChatMemoryStore(dir);
        assertTrue(store.getMessages("STU-145").isEmpty());
        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("STU-145.json.corrupt-")));
        }
    }

    @Test
    void deleteRemovesTheFile() {
        FileChatMemoryStore store = new FileChatMemoryStore(dir);
        store.updateMessages("u1", List.of(UserMessage.from("x")));
        store.deleteMessages("u1");
        assertTrue(store.getMessages("u1").isEmpty());
    }
}
