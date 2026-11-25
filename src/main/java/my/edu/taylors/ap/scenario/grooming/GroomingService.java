package my.edu.taylors.ap.scenario.grooming;

import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.grooming.GroomingExceptions.SlotUnavailableException;
import my.edu.taylors.ap.scenario.grooming.GroomingExceptions.UnknownBreedException;
import my.edu.taylors.ap.scenario.grooming.GroomingExceptions.UnknownServiceException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/** Deterministic rules P1-P4. */
public class GroomingService {

    private final List<GroomService> services;
    private final List<Breed> breeds;
    private final List<Groomer> groomers;
    private final GroomingRules rules;

    public GroomingService(List<GroomService> services, List<Breed> breeds, List<Groomer> groomers, GroomingRules rules) {
        this.services = List.copyOf(services);
        this.breeds = List.copyOf(breeds);
        this.groomers = List.copyOf(groomers);
        this.rules = rules;
    }

    public List<GroomService> services() {
        return services;
    }

    public String sizeOf(String breed) throws UnknownBreedException {
        String b = breed == null ? "" : breed.trim().toLowerCase(Locale.ROOT);
        return breeds.stream().filter(x -> x.breed().toLowerCase(Locale.ROOT).equals(b)).map(Breed::size)
                .findFirst().orElseThrow(() -> new UnknownBreedException(breed));
    }

    public GroomService service(String code) throws UnknownServiceException {
        return services.stream().filter(s -> s.code().equalsIgnoreCase(code == null ? "" : code.trim()))
                .findFirst().orElseThrow(() -> new UnknownServiceException(code));
    }

    /** P1 duration (rounded UP to 15 min), P2 surcharge, P3 discount on base only. */
    public Estimate estimate(String breed, String serviceCode, boolean firstVisit) throws RuleViolationException {
        String size = sizeOf(breed);
        GroomService s = service(serviceCode);
        long raw = (long) s.baseMinutes() * rules.factorTenths(size);        // minutes x 10
        int minutes = (int) ((raw + 149) / 150 * 15);                        // ceil(raw/10/15) * 15
        long surcharge = rules.surchargeSen(size);
        long discount = firstVisit ? Money.percentOf(s.basePriceSen(), rules.firstVisitPercent()) : 0;
        return new Estimate(s.code(), size, minutes, s.basePriceSen(), surcharge, discount,
                s.basePriceSen() + surcharge - discount);
    }

    /** P4: first groomer (in file order) who handles the size and is free; XL only at 09:00. */
    public Groomer assignGroomer(String size, LocalDate date, LocalTime start, int minutes, List<Appointment> existing)
            throws RuleViolationException {
        int s = start.getHour() * 60 + start.getMinute();
        if (s < GroomingRules.OPEN_MINUTE || s + minutes > GroomingRules.CLOSE_MINUTE) {
            throw new SlotUnavailableException("P4", "Appointments must fit between 09:00 and 18:00.");
        }
        if ("XL".equals(size) && s != GroomingRules.OPEN_MINUTE) {
            throw new SlotUnavailableException("P4", "XL dogs can only start at 09:00.");
        }
        LocalTime end = start.plusMinutes(minutes);
        int need = GroomingRules.SIZES.indexOf(size);
        for (Groomer g : groomers) {
            if (GroomingRules.SIZES.indexOf(g.maxSize()) < need) {
                continue;
            }
            boolean busy = existing.stream().anyMatch(a -> a.overlaps(g.name(), date, start, end));
            if (!busy) {
                return g;
            }
        }
        throw new SlotUnavailableException("P4", "No groomer who handles size " + size + " is free at " + start + " on " + date + ".");
    }
}
