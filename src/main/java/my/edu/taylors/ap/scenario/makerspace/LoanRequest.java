package my.edu.taylors.ap.scenario.makerspace;

import dev.langchain4j.model.output.structured.Description;

/** STRUCTURED OUTPUT target for "I need a soldering iron for my IoT project tonight". */
@Description("a request to borrow makerspace equipment")
public record LoanRequest(
        @Description("equipment code from the catalogue, e.g. EQ-SOL-01") String equipmentCode,
        @Description("short purpose of the loan, max 10 words") String purpose) { }
