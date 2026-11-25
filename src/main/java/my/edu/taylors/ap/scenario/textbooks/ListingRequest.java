package my.edu.taylors.ap.scenario.textbooks;

import dev.langchain4j.model.output.structured.Description;

/** STRUCTURED OUTPUT target for "selling my Liang Java book, 11th ed, some highlighting, RM45". */
@Description("a request to list a used textbook for sale")
public record ListingRequest(
        @Description("ISBN-13 of the book, digits only, chosen from the catalogue") String isbn,
        @Description("condition grade A, B, C or D as defined in the grading guide") String condition,
        @Description("asking price in ringgit as a decimal string, e.g. 45.00") String askingPriceRm,
        @Description("description of defects such as missing pages or heavy notes; empty if none") String defects) { }
