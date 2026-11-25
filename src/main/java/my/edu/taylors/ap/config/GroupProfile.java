package my.edu.taylors.ap.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything that is personalised for one group: its scenario, its seeded
 * house-rule parameters and its watermark token.
 *
 * @param groupNumber the group number assigned by the module leader (1-99)
 * @param scenario    the scenario derived from the group number
 * @param params      seeded parameters, keyed by the names used in the brief (e.g. "T", "N")
 * @param token       4-character watermark token shown in the UI and written to every trace line
 */
public record GroupProfile(int groupNumber, Scenario scenario, Map<String, Integer> params, String token) {

    public GroupProfile {
        params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /** e.g. "G07" */
    public String groupCode() {
        return String.format("G%02d", groupNumber);
    }

    /** e.g. "ITS66704-G07-K3QZ" — the string the watermark and the trace header carry. */
    public String watermarkId() {
        return "ITS66704-" + groupCode() + "-" + token;
    }

    /** Returns a seeded parameter or throws if the scenario does not define it. */
    public int param(String key) {
        Integer v = params.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Parameter '" + key + "' is not defined for " + scenario);
        }
        return v;
    }
}
