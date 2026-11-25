package my.edu.taylors.ap.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A generic, file-backed list of records - this project's replacement for a database table.
 * <p>
 * Each store owns ONE JSON file containing a JSON array. Writes go to a temporary file first
 * and are then moved over the original, so a crash never leaves a half-written file.
 *
 * <pre>{@code
 * JsonStore<MenuItem> menu = JsonStore.of(dataDir.resolve("menu.json"), MenuItem.class);
 * List<MenuItem> all = menu.findAll();
 * }</pre>
 *
 * @param <T> record/POJO type stored in the file (Java records are supported by Gson 2.10+)
 */
public final class JsonStore<T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;
    private final Type listType;

    private JsonStore(Path file, Type listType) {
        this.file = file;
        this.listType = listType;
    }

    public static <T> JsonStore<T> of(Path file, Class<T> elementType) {
        return new JsonStore<>(file, TypeToken.getParameterized(List.class, elementType).getType());
    }

    /**
     * Store whose LIVE file lives under runtime/, initialised once from a SEED file in the
     * scenario's data/ folder. The seed data is never modified, so you can always reset by
     * deleting the runtime file.
     */
    public static <T> JsonStore<T> seeded(Path seed, Path live, Class<T> elementType) throws DataAccessException {
        if (!Files.exists(live) && Files.exists(seed)) {
            try {
                Files.createDirectories(live.toAbsolutePath().getParent());
                Files.copy(seed, live);
            } catch (IOException e) {
                throw new DataAccessException("Cannot seed " + live.getFileName() + ": " + e.getMessage(), e);
            }
        }
        return of(live, elementType);
    }

    public Path file() {
        return file;
    }

    /** Returns all records; an empty list if the file does not exist yet. */
    public synchronized List<T> findAll() throws DataAccessException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<T> items = GSON.fromJson(in, listType);
            return items == null ? new ArrayList<>() : new ArrayList<>(items);
        } catch (IOException | JsonParseException e) {
            throw new DataAccessException("Cannot read " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    public List<T> findWhere(Predicate<? super T> filter) throws DataAccessException {
        return findAll().stream().filter(filter).toList();
    }

    /** Replaces the whole file atomically. */
    public synchronized void saveAll(List<T> items) throws DataAccessException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            try (Writer out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(items, listType, out);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DataAccessException("Cannot write " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    public synchronized void append(T item) throws DataAccessException {
        List<T> all = findAll();
        all.add(item);
        saveAll(all);
    }

    /** Shared Gson instance for other classes that need JSON (e.g. printing a POJO). */
    public static Gson gson() {
        return GSON;
    }
}
