package my.edu.taylors.ap.config;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Renders a group's house rules as plain sentences, with the seeded values filled in.
 * The same text is (a) shown in the UI, (b) injected into the agent's system message and
 * (c) printed on the group's house-rules card. Your Java code must ENFORCE these rules;
 * the system message only helps the model explain them.
 */
public final class HouseRules {

    private HouseRules() { }

    public static String rm(int sen) {
        return String.format("RM%d.%02d", sen / 100, sen % 100);
    }

    public static String clock(int minutesAfterMidnight) {
        return String.format("%02d:%02d", minutesAfterMidnight / 60, minutesAfterMidnight % 60);
    }

    public static String tenths(int tenths) {
        return (tenths / 10) + "." + (tenths % 10);
    }

    public static List<String> forProfile(GroupProfile g) {
        return switch (g.scenario()) {
            case KOPITIAM -> List.of(
                    "K1 Afternoon treat: an order placed from 15:00 to 17:59 whose subtotal is at least "
                            + rm(g.param("T")) + " receives ONE free Kaya Toast (item K01). Maximum one free item per order.",
                    "K2 Rush-hour cap: on weekdays (Mon-Fri) before " + clock(g.param("H"))
                            + ", an order may contain at most " + g.param("N") + " Teh Tarik (item D01).",
                    "K3 Charges: dine-in orders pay a " + g.param("S") + "% service charge on the subtotal "
                            + "(rounded half-up to the nearest sen). Takeaway orders pay RM0.20 per drink unit for containers instead.",
                    "K4 Rounding: the final total is rounded to the nearest 5 sen (Malaysian rounding mechanism).",
                    "K5 Allergen honesty: the agent must never claim an item is allergen-free or 'safe'. "
                            + "It must list the allergens recorded in the menu data and advise the customer to confirm with staff.");
            case MAKERSPACE -> List.of(
                    "M1 Safety: equipment in category SOLDERING or LASER may only be loaned to members whose safety quiz is passed.",
                    "M2 Loan period: the standard loan is " + g.param("B") + " hours. Members with an ODD numeric member ID get 12 extra hours. "
                            + "3D printers (category PRINTER3D) are always limited to 8 hours.",
                    "M3 Limit: a member may hold at most " + g.param("C") + " active loans at the same time.",
                    "M4 Late fee: " + g.param("F") + " sen per started hour late, capped at RM30.00 per loan.",
                    "M5 Privacy: the agent must never reveal another member's name, loans or quiz status.");
            case ROOMS -> List.of(
                    "R1 Blocked day: rooms whose code ends in an ODD digit cannot be booked on "
                            + DayOfWeek.of(g.param("W")).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ".",
                    "R2 Slots: bookings start on the hour or half hour, between 08:00 and 22:00, last 30 minutes to "
                            + g.param("L") + " hours, and must be made at least " + g.param("A") + " hours in advance.",
                    "R3 Capacity: the group size may not exceed " + g.param("P") + "% of the room capacity (rounded down).",
                    "R4 Conflicts: a booking may not overlap an existing booking for the same room.",
                    "R5 No phantom bookings: the agent may only say a booking is confirmed if a booking ID (BK-nnnn) was actually issued.");
            case GROOMING -> List.of(
                    "P1 Duration: service time = base minutes x size factor (S 1.0, M 1.2, L " + tenths(g.param("LF"))
                            + ", XL " + tenths(g.param("XF")) + "), rounded UP to the next 15 minutes.",
                    "P2 Price: base price + size surcharge (S and M: none, L: " + rm(g.param("X"))
                            + ", XL: " + rm(2 * g.param("X")) + ").",
                    "P3 First visit: a pet's first visit gets " + g.param("D") + "% off the base price only (not the surcharge), rounded half-up to the sen.",
                    "P4 Scheduling: appointments run 09:00-18:00 and may not overlap for the same groomer. XL dogs may only start at 09:00.",
                    "P5 No vet advice: the agent must not diagnose or recommend medication; it must refer health questions to a veterinarian.");
            case TEXTBOOKS -> List.of(
                    "T1 Suggested price: list price x (1 - " + g.param("Y") + "%)^age x condition factor (A 1.00, B 0.85, C 0.65, D 0.40); "
                            + "a superseded edition loses a further " + g.param("Z") + "%; never below " + rm(g.param("F"))
                            + "; rounded DOWN to the nearest RM0.50.",
                    "T2 Fair asking price: a seller may ask at most 20% above the suggested price.",
                    "T3 Buyer fee: the buyer pays the asking price plus a " + g.param("P") + "% platform fee (rounded half-up to the sen).",
                    "T4 Disclosure: condition D listings must include a non-empty 'defects' description.",
                    "T5 Privacy: the agent must never reveal a seller's email address or phone number; meet-ups are arranged through the platform.");
        };
    }
}
