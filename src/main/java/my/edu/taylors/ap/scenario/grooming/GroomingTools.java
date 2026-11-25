package my.edu.taylors.ap.scenario.grooming;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.DateTimes;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.grooming.GroomingExceptions.UnknownPetException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/** Grooming tools. Owners act only on their own pets (@ToolMemoryId = owner ID). */
public class GroomingTools {

    private final GroomingService service;
    private final JsonStore<Pet> pets;
    private final JsonStore<Appointment> appointments;

    public GroomingTools(GroomingService service, JsonStore<Pet> pets, JsonStore<Appointment> appointments) {
        this.service = service;
        this.pets = pets;
        this.appointments = appointments;
    }

    @Tool("List grooming services with codes, base duration and base price.")
    public String listServices() {
        return service.services().stream()
                .map(s -> s.code() + " " + s.name() + " " + s.baseMinutes() + " min, from " + Money.rm(s.basePriceSen()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("List the current owner's registered pets.")
    public String myPets(@ToolMemoryId String ownerId) throws DataAccessException {
        List<Pet> mine = pets.findWhere(p -> p.ownerId().equalsIgnoreCase(ownerId));
        return mine.isEmpty() ? "You have no registered pets." : mine.stream()
                .map(p -> p.petId() + " " + p.name() + " (" + p.breed() + ", " + p.visits() + " previous visits)")
                .collect(Collectors.joining("\n"));
    }

    @Tool("Estimate duration and price for a breed and service. Use firstVisit=true for new pets.")
    public String estimate(@P("breed exactly as in our table, e.g. Golden Retriever") String breed,
                           @P("service code, e.g. FULL") String serviceCode,
                           @P("true if this is the pet's first visit") boolean firstVisit) throws RuleViolationException {
        return service.estimate(breed, serviceCode, firstVisit).describe();
    }

    @Tool("Book a grooming appointment for one of the current owner's pets. Returns the appointment ID.")
    public String book(@ToolMemoryId String ownerId,
                       @P("pet ID, e.g. PET-003") String petId,
                       @P("service code") String serviceCode,
                       @P("date yyyy-MM-dd") String date,
                       @P("start time HH:mm") String startTime) throws RuleViolationException, DataAccessException {
        Pet pet = pets.findWhere(p -> p.petId().equalsIgnoreCase(petId) && p.ownerId().equalsIgnoreCase(ownerId))
                .stream().findFirst().orElseThrow(() -> new UnknownPetException(petId));
        Estimate est = service.estimate(pet.breed(), serviceCode, pet.visits() == 0);
        LocalDate d = DateTimes.parseDate(date);
        LocalTime t = DateTimes.parseTime(startTime);
        List<Appointment> all = appointments.findAll();
        Groomer g = service.assignGroomer(est.size(), d, t, est.minutes(), all);
        Appointment a = new Appointment(String.format("GR-%04d", all.size() + 1), pet.petId(), est.serviceCode(), g.name(),
                d.toString(), t.toString(), t.plusMinutes(est.minutes()).toString(), est.totalSen(), ownerId);
        appointments.append(a);
        return "Appointment " + a.appointmentId() + " booked with " + g.name() + " on " + a.date() + " " + a.start()
                + "-" + a.end() + ". Price " + Money.rm(a.priceSen()) + ".";
    }
}
