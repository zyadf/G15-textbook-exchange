// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.guardrail.ScopeInputGuardrail;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Scenario 5 (Textbook Exchange) wiring for group 15. The application calls these methods; everything scenario-specific
 * is created here. Work through the TODOs in the order given in the assignment brief.
 */
public class MyScenarioModule implements ScenarioModule {

    static final String QUILLMARK = "generated";

    private final GroupProfile profile;
    private final Path dataDir;
    private final Path runtimeDir;
    private final ChatModel model;
    private final TraceLogger trace;
    private final Clock clock;
    private final ExampleTools exampleTools;
    private final ExchangeService service;
    private final JsonStore<Listing> listings;
    private final CatalogueTools catalogueTools;
    private final ListingTools listingTools;
    private final BuyerTools buyerTools;
    private final PriceTools priceTools;
    private final InterestTools interestTools;

    public MyScenarioModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                            TraceLogger trace, Clock clock) throws DataAccessException {
        this.profile = profile;
        this.dataDir = dataDir;
        this.runtimeDir = runtimeDir;
        this.model = model;
        this.trace = trace;
        this.clock = clock;
        this.exampleTools = new ExampleTools(profile, clock, dataDir);
        // [A6] books.json is read-only seed data -> read it straight from data/.
        //      listings.json is CHANGED by createListing -> JsonStore.seeded copies it to runtime/ once,
        //      so the seed file in scenarios/ is never modified (delete runtime/listings.json to reset).
        List<Book> books = JsonStore.of(dataDir.resolve("books.json"), Book.class).findAll();
        this.service = new ExchangeService(books, ExchangeRules.from(profile), clock);
        this.listings = JsonStore.seeded(dataDir.resolve("listings.json"), runtimeDir.resolve("listings.json"), Listing.class);
        this.catalogueTools = new CatalogueTools(service);
        this.listingTools = new ListingTools(service, listings);
        this.buyerTools = new BuyerTools(service, listings);
        this.priceTools = new PriceTools(service);
        this.interestTools = new InterestTools(listings);
        // TODO [B3] create your structured-output extractor:
        //           AiServices.builder(YourExtractor.class).chatModel(model).build();
    }

    @Override
    public Scenario scenario() {
        return profile.scenario();
    }

    @Override
    public List<Object> tools() {
        List<Object> tools = new ArrayList<>();
        tools.add(exampleTools);
        tools.add(catalogueTools);   // searchBooks
        tools.add(priceTools);       // suggestPrice                    (T1, T2)
        tools.add(listingTools);     // createListing, myListings       (T2, T4)
        tools.add(buyerTools);       // findListings, buyerQuote        (T3)
        tools.add(interestTools);    // expressInterest                 (T5 by design)
        return tools;
    }

    @Override
    public List<InputGuardrail> inputGuardrails() {
        // TODO [B4] add further input guardrails from your threat model (Part A, A5)
        return List.of(new ScopeInputGuardrail(trace));
    }

    @Override
    public List<OutputGuardrail> outputGuardrails() {
        // TODO [B4] replace with your guardrail for the fifth house rule
        return List.of(new MyOutputGuardrail(trace));
    }

    @Override
    public String structuredActionLabel() {
        return "Extract request";      // TODO [B3] e.g. "Extract order", "Check booking"
    }

    @Override
    public String defaultUserId() {
        return "STU-145";              // [A6] a seller in listings.json who owns TX-0001 and TX-0005
    }

    @Override
    public List<Path> dataFiles() {
        // TODO [B6] check this list when you build the Data tab enhancement
        return List.of(listings.file(), dataDir.resolve("books.json"), dataDir.resolve("listings.json"));
    }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        // TODO [B3] 1. call your extractor AI Service -> POJO
        //           2. trace.log("structured", Map.of("pojo", String.valueOf(pojo)))
        //           3. validate the POJO with your service (house rules) - throw RuleViolationException subclasses
        //           4. return new StructuredResult(summary, () -> yourTools.commitSomething(...))
        throw new RuleViolationException("TODO", "Structured output is not implemented yet (Part B, task B3).");
    }
}
