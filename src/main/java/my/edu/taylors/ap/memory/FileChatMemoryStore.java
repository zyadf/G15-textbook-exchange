package my.edu.taylors.ap.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO [A6] Persist each user's conversation to {@code runtime/memory/<userId>.json} so that it
 * survives a restart.
 * <p>
 * CURRENT BEHAVIOUR: messages are kept in a HashMap and lost when the app closes.
 * Replace the map with files. Useful API:
 * {@code ChatMessageSerializer.messagesToJson(messages)} and
 * {@code ChatMessageDeserializer.messagesFromJson(json)}.
 * Think about: unsafe characters in user ids (path traversal), half-written files if the app
 * crashes, and what to do with a corrupted file. Your design report must justify your choices.

 * TODO [A6] COMPLETED
 * Persists each user's conversation in its own JSON file: {@code runtime/memory/<userId>.json}.
 * <p>
 * LangChain4j calls {@link #updateMessages} after every exchange with the FULL current window
 * of messages (MessageWindowChatMemory evicts old ones first), so this store simply overwrites
 * the file. Restarting the app restores the conversation.
 */
public class FileChatMemoryStore implements ChatMemoryStore {

    private final Path dir;
    // private final Map<String, List<ChatMessage>> temporary = new ConcurrentHashMap<>();   // TODO [A6] remove

    public FileChatMemoryStore(Path dir) {
        this.dir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create memory directory " + dir, e);
        }
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // TODO [A6] read runtime/memory/<id>.json if it exists
        // return new ArrayList<>(temporary.getOrDefault(String.valueOf(memoryId), List.of()));

        Path file = fileFor(memoryId);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return new ArrayList<>(ChatMessageDeserializer.messagesFromJson(json));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read memory for " + memoryId, e);
        } catch (RuntimeException e) {
            // Corrupted file: keep a copy for the failure catalogue, then start a fresh conversation.
            quarantine(file);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // TODO [A6] write the whole list to runtime/memory/<id>.json
        // temporary.put(String.valueOf(memoryId), new ArrayList<>(messages));

        Path file = fileFor(memoryId);
        try {
            Path tmp = Files.createTempFile(dir, "mem", ".tmp");
            Files.writeString(tmp, ChatMessageSerializer.messagesToJson(messages), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save memory for " + memoryId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        // TODO [A6] delete the file
        // temporary.remove(String.valueOf(memoryId));

        try {
            Files.deleteIfExists(fileFor(memoryId));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete memory for " + memoryId, e);
        }
    }

    /* TODO [A6] ADDED fileFor method */
    /** Maps a memory id to a safe file name (prevents path traversal such as "../../x"). */
    Path fileFor(Object memoryId) {
        String safe = String.valueOf(memoryId).replaceAll("[^A-Za-z0-9_-]", "_");
        if (safe.isBlank()) {
            safe = "default";
        }
        return dir.resolve(safe + ".json");
    }

    private void quarantine(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
