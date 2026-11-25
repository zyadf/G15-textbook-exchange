package my.edu.taylors.ap.scenario.grooming;

import my.edu.taylors.ap.config.GroupProfile;

import java.util.List;

/**
 * @param largeFactorTenths  P1 size factor for L (tenths, e.g. 14 = 1.4)
 * @param xlFactorTenths     P1 size factor for XL (tenths)
 * @param largeSurchargeSen  P2 surcharge X for L (XL pays 2X)
 * @param firstVisitPercent  P3 discount D
 */
public record GroomingRules(int largeFactorTenths, int xlFactorTenths, int largeSurchargeSen, int firstVisitPercent) {

    public static final List<String> SIZES = List.of("S", "M", "L", "XL");
    public static final int OPEN_MINUTE = 9 * 60;
    public static final int CLOSE_MINUTE = 18 * 60;

    public static GroomingRules from(GroupProfile g) {
        return new GroomingRules(g.param("LF"), g.param("XF"), g.param("X"), g.param("D"));
    }

    public int factorTenths(String size) {
        return switch (size) {
            case "S" -> 10;
            case "M" -> 12;
            case "L" -> largeFactorTenths;
            case "XL" -> xlFactorTenths;
            default -> throw new IllegalArgumentException("Unknown size " + size);
        };
    }

    public int surchargeSen(String size) {
        return switch (size) {
            case "L" -> largeSurchargeSen;
            case "XL" -> 2 * largeSurchargeSen;
            default -> 0;
        };
    }
}
