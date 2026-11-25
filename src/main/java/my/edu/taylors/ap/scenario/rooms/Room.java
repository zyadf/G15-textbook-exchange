package my.edu.taylors.ap.scenario.rooms;

/** Row of data/rooms.json. Types: LAB, SEMINAR, DISCUSSION. */
public record Room(String code, String type, int capacity, boolean projector) {

    /** R1: "C7.03" ends in 3 -> odd. */
    public boolean endsInOddDigit() {
        char last = code.charAt(code.length() - 1);
        return Character.isDigit(last) && (last - '0') % 2 == 1;
    }
}
