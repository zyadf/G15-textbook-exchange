package my.edu.taylors.ap.scenario.textbooks;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.guardrail.ScopeInputGuardrail;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Scenario 5 wiring. */
public class TextbooksModule implements ScenarioModule {

    interface ListingExtractor {
        @UserMessage("""
                Extract the textbook listing from the seller's message. Catalogue: {{catalogue}}
                Message: {{text}}""")
        ListingRequest extract(@V("text") String text, @V("catalogue") String catalogue);
    }

    private final TextbookService service;
    private final TextbookTools tools;
    private final ListingExtractor extractor;
    private final TraceLogger trace;
    private final JsonStore<Listing> listings;
    private final Path dataDir;

    public TextbooksModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                           TraceLogger trace, Clock clock) throws DataAccessException {
        this.service = new TextbookService(JsonStore.of(dataDir.resolve("books.json"), Book.class).findAll(),
                TextbookRules.from(profile), clock);
        this.listings = JsonStore.seeded(dataDir.resolve("listings.json"), runtimeDir.resolve("listings.json"), Listing.class);
        this.tools = new TextbookTools(service, listings);
        this.extractor = AiServices.builder(ListingExtractor.class).chatModel(model).build();
        this.trace = trace;
        this.dataDir = dataDir;
    }

    @Override public Scenario scenario() { return Scenario.TEXTBOOKS; }
    @Override public List<Object> tools() { return List.of(tools); }
    @Override public List<InputGuardrail> inputGuardrails() { return List.of(new ScopeInputGuardrail(trace)); }
    @Override public List<OutputGuardrail> outputGuardrails() { return List.of(new ContactPrivacyGuardrail(trace)); }
    @Override public String structuredActionLabel() { return "Check listing"; }
    @Override public String defaultUserId() { return "STU-201"; }
    @Override public List<Path> dataFiles() { return List.of(listings.file(), dataDir.resolve("books.json")); }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        String catalogue = service.books().stream().map(b -> b.isbn() + "=" + b.title() + " ed." + b.edition())
                .collect(Collectors.joining("; "));
        ListingRequest req = extractor.extract(userText, catalogue);
        trace.log("structured", Map.of("pojo", String.valueOf(req)));
        if (req == null || req.isbn() == null) {
            throw new RuleViolationException("FORMAT", "Could not identify the book.");
        }
        Book b = service.book(req.isbn());
        long asking = TextbookService.parseRinggit(req.askingPriceRm());
        service.validateListing(b, req.condition(), asking, req.defects());
        long suggested = service.suggestedSen(b, req.condition());
        return new StructuredResult("Extracted: " + JsonStore.gson().toJson(req) + "\nValid listing. Suggested " + Money.rm(suggested)
                + ", buyer would pay " + Money.rm(service.buyerTotalSen(asking)) + " (not listed yet).",
                () -> tools.createListing(userId, req.isbn(), req.condition(), req.askingPriceRm(), req.defects()));
    }
}
