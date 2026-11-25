package my.edu.taylors.ap.scenario.kopitiam;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

/**
 * STRUCTURED OUTPUT target: the LLM fills this POJO from free text such as
 * "2 teh tarik and a roti canai, makan sini". LangChain4j derives a JSON schema from the record.
 */
@Description("A customer's kopitiam order")
public record OrderRequest(
        @Description("ordered items; use menu item codes such as D01, never item names") List<OrderLine> lines,
        @Description("true if the customer eats in the shop (dine-in / 'makan sini'), false for takeaway ('bungkus' / 'tapau')") boolean dineIn,
        @Description("customer's first name if given, otherwise empty") String customerName) {

    @Description("one line of an order")
    public record OrderLine(
            @Description("menu item code, e.g. D01") String itemCode,
            @Description("number of units, at least 1") int quantity) { }
}
