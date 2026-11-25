package my.edu.taylors.ap.scenario.grooming;

import my.edu.taylors.ap.scenario.common.Money;

/** P1-P3 result. */
public record Estimate(String serviceCode, String size, int minutes, long basePriceSen, long surchargeSen,
                       long discountSen, long totalSen) {

    public String describe() {
        return String.format("%s for size %s: %d minutes. Base %s + surcharge %s - first-visit discount %s = %s",
                serviceCode, size, minutes, Money.rm(basePriceSen), Money.rm(surchargeSen), Money.rm(discountSen), Money.rm(totalSen));
    }
}
