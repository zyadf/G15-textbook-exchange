package my.edu.taylors.ap.scenario.grooming;

import my.edu.taylors.ap.scenario.common.RuleViolationException;

/** Custom checked exceptions for the grooming scenario. */
public final class GroomingExceptions {

    private GroomingExceptions() { }

    public static class UnknownBreedException extends RuleViolationException {
        public UnknownBreedException(String breed) {
            super("BREED", "Breed '" + breed + "' is not in our size table. Ask staff to add it.");
        }
    }

    public static class UnknownServiceException extends RuleViolationException {
        public UnknownServiceException(String code) {
            super("SERVICE", "No grooming service with code '" + code + "'.");
        }
    }

    public static class UnknownPetException extends RuleViolationException {
        public UnknownPetException(String petId) {
            super("PET", "No pet with ID '" + petId + "' is registered to you.");
        }
    }

    public static class SlotUnavailableException extends RuleViolationException {
        public SlotUnavailableException(String ruleId, String detail) {
            super(ruleId, detail);
        }
    }
}
