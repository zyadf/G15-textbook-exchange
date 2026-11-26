// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Look-up tool: find a book by title keyword or ISBN. Read-only, so no user id is needed.
 * Tools stay THIN: validate the arguments, call {@link ExchangeService}, return short text.
 */
public class CatalogueTools {

    static final String QUILLMARK = "generated";

    private final ExchangeService service;

    public CatalogueTools(ExchangeService service) {
        this.service = service;
    }

    @Tool("Search the textbook catalogue by a title keyword or ISBN. Returns ISBN, title, edition, year and "
            + "whether the edition is superseded. Use it to find the ISBN before calling any other tool.")
    public String searchBooks(@P("title keyword (e.g. 'java') or ISBN; empty string lists every book") String keyword) {
        List<Book> hits = service.search(keyword);
        if (hits.isEmpty()) {
            return "No books match '" + keyword + "'.";
        }
        return hits.stream()
                .map(b -> b.isbn() + " | " + b.title() + " | ed. " + b.edition() + " | " + b.year()
                        + (b.isSuperseded() ? " | superseded by " + b.supersededBy() : ""))
                .collect(Collectors.joining("\n"));
    }
}
