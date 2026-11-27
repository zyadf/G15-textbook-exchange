// AI-ASSISTED: Claude (Anthropic), 2026-11-26
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.RuleViolationException;

/**
 * Pricing tool: the suggested resale price (T1) and the maximum asking price (T2) for one book.
 * All arithmetic is done in {@link ExchangeService}; the model must never calculate prices itself.
 */
public class PriceTools {

    static final String QUILLMARK = "generated";

    private final ExchangeService service;

    public PriceTools(ExchangeService service) {
        this.service = service;
    }

    @Tool("Suggested resale price (house rule T1) and the maximum asking price (T2) for one book in a "
            + "condition grade. Always use this tool for prices; never calculate them yourself.")
    public String suggestPrice(@P("ISBN-13 from searchBooks") String isbn,
                               @P("condition grade: A, B, C or D") String condition) throws RuleViolationException {
        return service.suggest(service.book(isbn), condition).describe();
    }
}
