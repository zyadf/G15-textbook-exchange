package my.edu.taylors.ap.scenario.rooms;

import dev.langchain4j.model.output.structured.Description;

/** STRUCTURED OUTPUT target for "book a discussion room for 6 of us next Tuesday 2-4pm". */
@Description("a room booking request")
public record BookingRequest(
        @Description("room code such as C7.03, or empty if the user did not name a room") String roomCode,
        @Description("date in ISO format yyyy-MM-dd, resolved relative to today") String date,
        @Description("start time HH:mm in 24-hour format") String startTime,
        @Description("duration in minutes") int durationMinutes,
        @Description("number of people") int groupSize) { }
