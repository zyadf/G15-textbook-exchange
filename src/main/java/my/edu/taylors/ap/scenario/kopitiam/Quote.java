package my.edu.taylors.ap.scenario.kopitiam;

import my.edu.taylors.ap.scenario.common.Money;

import java.util.List;

/** Result of pricing an order. All amounts in sen. */
public record Quote(
        List<Line> lines,
        long subtotalSen,
        boolean freeToastApplied,
        long serviceChargeSen,
        long containerFeeSen,
        long unroundedSen,
        long totalSen,
        boolean dineIn) {

    public record Line(String code, String name, int quantity, long unitSen, long lineSen) { }

    /** Text the tool returns to the model (and the UI shows). */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) {
            sb.append(String.format("%dx %s (%s) %s%n", l.quantity(), l.name(), l.code(),
                    l.lineSen() == 0 ? "FREE" : Money.rm(l.lineSen())));
        }
        sb.append("Subtotal: ").append(Money.rm(subtotalSen)).append('\n');
        if (freeToastApplied) {
            sb.append("K1 afternoon treat: 1 free Kaya Toast added\n");
        }
        if (dineIn) {
            sb.append("Service charge (K3): ").append(Money.rm(serviceChargeSen)).append('\n');
        } else {
            sb.append("Container fee (K3): ").append(Money.rm(containerFeeSen)).append('\n');
        }
        sb.append("Before rounding: ").append(Money.rm(unroundedSen)).append('\n');
        sb.append("TOTAL (K4, rounded to 5 sen): ").append(Money.rm(totalSen));
        return sb.toString();
    }
}
