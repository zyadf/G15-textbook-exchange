package my.edu.taylors.ap.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Deterministically derives a {@link GroupProfile} from a group number.
 * <p>
 * The same group number ALWAYS produces the same scenario, parameters and token on every
 * machine, because {@link java.util.Random} uses a fully specified algorithm.
 * <p>
 * Usage (prints your house-rules card):
 * <pre>mvn -q compile exec:java -Dexec.mainClass=my.edu.taylors.ap.config.GroupSeeder -Dexec.args="7"</pre>
 * Lecturer table: {@code -Dexec.args="--csv 1 80"}
 */
public final class GroupSeeder {

    static final long SEED_BASE = 66_704L * 1_009L;
    private static final String TOKEN_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private GroupSeeder() { }

    public static GroupProfile profileFor(int groupNumber) {
        if (groupNumber < 1 || groupNumber > 99) {
            throw new IllegalArgumentException("Group number must be between 1 and 99, got " + groupNumber);
        }
        Scenario scenario = Scenario.values()[(groupNumber - 1) % Scenario.values().length];
        Random r = new Random(SEED_BASE + groupNumber);
        Map<String, Integer> p = new LinkedHashMap<>();
        switch (scenario) {
            case KOPITIAM -> {
                p.put("T", 1800 + 10 * r.nextInt(180));          // free-toast threshold, sen (RM18.00-35.90)
                p.put("N", 3 + r.nextInt(4));                     // rush-hour teh tarik cap (3-6)
                p.put("H", 510 + 15 * r.nextInt(3));              // rush-hour end, minutes after midnight (08:30/08:45/09:00)
                p.put("S", 5 + r.nextInt(4));                     // dine-in service charge % (5-8)
            }
            case MAKERSPACE -> {
                p.put("B", 24 + 12 * r.nextInt(5));               // base loan hours (24-72)
                p.put("C", 2 + r.nextInt(3));                     // max concurrent loans (2-4)
                p.put("F", 20 + 5 * r.nextInt(9));                // late fee sen per started hour (20-60)
            }
            case ROOMS -> {
                p.put("W", 1 + r.nextInt(5));                     // blocked weekday for odd rooms (1=MON..5=FRI)
                p.put("L", 2 + r.nextInt(3));                     // max booking hours (2-4)
                p.put("P", 80 + 5 * r.nextInt(4));                // capacity percentage usable (80-95)
                int[] lead = {2, 4, 6, 12, 24};
                p.put("A", lead[r.nextInt(lead.length)]);         // minimum advance notice, hours
            }
            case GROOMING -> {
                int large = 13 + r.nextInt(4);                    // size factor L in tenths (1.3-1.6)
                p.put("LF", large);
                p.put("XF", large + 3);                           // size factor XL = L + 0.3
                p.put("X", 1000 + 500 * r.nextInt(5));            // size surcharge for L, sen (RM10-30); XL pays 2X
                p.put("D", 5 + r.nextInt(11));                    // first-visit discount % (5-15)
            }
            case TEXTBOOKS -> {
                p.put("Y", 8 + r.nextInt(8));                     // depreciation % per year (8-15)
                p.put("Z", 10 + 5 * r.nextInt(3));                // superseded-edition penalty % (10-20)
                p.put("F", 500 + 100 * r.nextInt(6));             // floor price, sen (RM5-10)
                p.put("P", 3 + r.nextInt(5));                     // platform fee % (3-7)
            }
        }
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            token.append(TOKEN_ALPHABET.charAt(r.nextInt(TOKEN_ALPHABET.length())));
        }
        return new GroupProfile(groupNumber, scenario, p, token.toString());
    }

    public static void main(String[] args) {
        if (args.length == 3 && args[0].equals("--csv")) {
            int from = Integer.parseInt(args[1]);
            int to = Integer.parseInt(args[2]);
            System.out.println("group,scenario,watermark,params");
            for (int g = from; g <= to; g++) {
                GroupProfile gp = profileFor(g);
                System.out.println(gp.groupCode() + "," + gp.scenario().id() + "," + gp.watermarkId() + ",\""
                        + gp.params().toString().replace("{", "").replace("}", "") + "\"");
            }
            return;
        }
        int group = args.length > 0 ? Integer.parseInt(args[0]) : AppConfig.load().groupNumber();
        GroupProfile gp = profileFor(group);
        System.out.println("Group      : " + gp.groupCode());
        System.out.println("Scenario   : " + gp.scenario().id() + " - " + gp.scenario().title());
        System.out.println("Watermark  : " + gp.watermarkId());
        System.out.println("House rules:");
        int i = 1;
        for (String rule : HouseRules.forProfile(gp)) {
            System.out.println("  " + (i++) + ". " + rule);
        }
    }
}
