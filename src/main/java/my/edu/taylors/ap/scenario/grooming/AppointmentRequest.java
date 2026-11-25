package my.edu.taylors.ap.scenario.grooming;

import dev.langchain4j.model.output.structured.Description;

/** STRUCTURED OUTPUT target for "full groom for my golden retriever next Saturday 10am, first time here". */
@Description("a grooming appointment request")
public record AppointmentRequest(
        @Description("dog or cat breed, using the breed table's spelling") String breed,
        @Description("service code: BATH, FULL, NAIL or DESHED") String serviceCode,
        @Description("date yyyy-MM-dd resolved relative to today") String date,
        @Description("start time HH:mm") String startTime,
        @Description("true if the owner says it is the first visit") boolean firstVisit) { }
