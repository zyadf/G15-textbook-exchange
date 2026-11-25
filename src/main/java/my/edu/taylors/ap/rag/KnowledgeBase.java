package my.edu.taylors.ap.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * TODO [B2] Retrieval-Augmented Generation over your scenario's {@code docs/} folder.
 * <p>
 * Steps (see the brief, section "LangChain4j primer - RAG"):
 * <ol>
 *   <li>Load every document: {@code FileSystemDocumentLoader.loadDocuments(docsDir, new TextDocumentParser())}</li>
 *   <li>Split into chunks: {@code DocumentSplitters.recursive(chars, overlap)} - justify your sizes</li>
 *   <li>Embed with the in-process model {@code new AllMiniLmL6V2EmbeddingModel()}</li>
 *   <li>Store vectors in an {@code InMemoryEmbeddingStore<TextSegment>} via {@code EmbeddingStoreIngestor}</li>
 *   <li>Persist: {@code store.serializeToFile(...)} to runtime/embeddings.json and reload it with
 *       {@code InMemoryEmbeddingStore.fromFile(...)} on the next start - but ONLY if the documents did not
 *       change (hint: store a SHA-256 fingerprint of the docs next to the file)</li>
 * </ol>
 *
 * TODO [B2] Completed
 * Retrieval-Augmented Generation (RAG) over the scenario's {@code docs/} folder.
 * <p>
 * Pipeline: load .md/.txt files -> split into overlapping chunks -> embed each chunk with an
 * in-process model (all-MiniLM-L6-v2) -> keep vectors in an {@link InMemoryEmbeddingStore}.
 * <p>
 * The store is serialised to {@code runtime/embeddings.json} together with a fingerprint of
 * the docs folder. On the next start the file is re-used unless the documents changed, which
 * avoids re-embedding everything on every launch.
 */
public class KnowledgeBase {
    /** Chunking parameters: justify your choice in the design report (Part A, A4). */
    public static final int CHUNK_CHARS = 600;
    public static final int OVERLAP_CHARS = 80;
    public static final int MAX_RESULTS = 3;
    public static final double MIN_SCORE = 0.55;
    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> store;

    private final boolean rebuilt;

    private KnowledgeBase(EmbeddingModel embeddingModel, InMemoryEmbeddingStore<TextSegment> store, boolean rebuilt) {
        this.embeddingModel = embeddingModel;
        this.store = store;
        this.rebuilt = rebuilt;
    }

    /**
     * Loads the persisted store if it is still valid for {@code docsDir}, otherwise rebuilds it.
     */
    public static KnowledgeBase openOrBuild(Path docsDir, Path storeFile) {
        // TODO [B2]
        //throw new UnsupportedOperationException("RAG is not implemented yet (Part B, task B2)");

        // TODO [B2] Completed
        EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();
        String fingerprint = fingerprint(docsDir);
        Path fingerprintFile = storeFile.resolveSibling(storeFile.getFileName() + ".sha256");
        try {
            if (Files.exists(storeFile) && Files.exists(fingerprintFile)
                    && Files.readString(fingerprintFile).trim().equals(fingerprint)) {
                return new KnowledgeBase(model, InMemoryEmbeddingStore.fromFile(storeFile), false);
            }
            InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
            List<Document> docs = FileSystemDocumentLoader.loadDocuments(docsDir, new TextDocumentParser());
            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(CHUNK_CHARS, OVERLAP_CHARS))
                    .embeddingModel(model)
                    .embeddingStore(store)
                    .build()
                    .ingest(docs);
            Files.createDirectories(storeFile.toAbsolutePath().getParent());
            store.serializeToFile(storeFile);
            Files.writeString(fingerprintFile, fingerprint);
            return new KnowledgeBase(model, store, true);
        } catch (IOException e) {
            throw new UncheckedIOException("Knowledge base I/O failed", e);
        }
    }

    /** The retriever that AiServices calls before every user message. */
    public ContentRetriever retriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3)        // TODO [B2] tune and justify maxResults / minScore
                .minScore(0.5)
                .build();
    }

    public boolean wasRebuilt() {
        return rebuilt;
    }

    /** SHA-256 over file names + contents of every document, in a stable order. */
    static String fingerprint(Path docsDir) {
        try (Stream<Path> files = Files.list(docsDir)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Path f : files.filter(Files::isRegularFile).sorted().toList()) {
                md.update(f.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                md.update(Files.readAllBytes(f));
            }
            md.update(("chunk=" + CHUNK_CHARS + "/" + OVERLAP_CHARS).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read docs folder " + docsDir, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
