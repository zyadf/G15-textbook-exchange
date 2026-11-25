package my.edu.taylors.ap.scenario.makerspace;

/** Row of data/equipment.json. Categories: TOOL, ELECTRONICS, SOLDERING, LASER, PRINTER3D. */
public record Equipment(String code, String name, String category, int quantity) {

    public boolean needsSafetyQuiz() {
        return "SOLDERING".equals(category) || "LASER".equals(category);
    }
}
