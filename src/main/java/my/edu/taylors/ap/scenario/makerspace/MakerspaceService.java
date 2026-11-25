package my.edu.taylors.ap.scenario.makerspace;

import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceExceptions.LoanLimitExceededException;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceExceptions.OutOfStockException;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceExceptions.SafetyQuizRequiredException;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceExceptions.UnknownEquipmentException;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceExceptions.UnknownMemberException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Deterministic loan rules M1-M4. Works on in-memory lists; the tools persist the results. */
public class MakerspaceService {

    private final Map<String, Equipment> equipment;
    private final Map<String, Member> members;
    private final MakerspaceRules rules;
    private final Clock clock;

    public MakerspaceService(List<Equipment> equipment, List<Member> members, MakerspaceRules rules, Clock clock) {
        this.equipment = equipment.stream().collect(Collectors.toMap(e -> e.code().toUpperCase(Locale.ROOT), Function.identity()));
        this.members = members.stream().collect(Collectors.toMap(m -> m.memberId().toUpperCase(Locale.ROOT), Function.identity()));
        this.rules = rules;
        this.clock = clock;
    }

    public Member member(String id) throws UnknownMemberException {
        Member m = members.get(id == null ? "" : id.trim().toUpperCase(Locale.ROOT));
        if (m == null) {
            throw new UnknownMemberException(id);
        }
        return m;
    }

    public Equipment equipment(String code) throws UnknownEquipmentException {
        Equipment e = equipment.get(code == null ? "" : code.trim().toUpperCase(Locale.ROOT));
        if (e == null) {
            throw new UnknownEquipmentException(code);
        }
        return e;
    }

    public List<Equipment> find(String keyword) {
        String k = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        return equipment.values().stream()
                .filter(e -> e.name().toLowerCase(Locale.ROOT).contains(k) || e.category().toLowerCase(Locale.ROOT).contains(k)
                        || e.code().toLowerCase(Locale.ROOT).contains(k))
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
    }

    /** M2: loan length in hours for this member and item. */
    public int loanHours(Member m, Equipment e) {
        if ("PRINTER3D".equals(e.category())) {
            return MakerspaceRules.PRINTER_HOURS;
        }
        return rules.baseLoanHours() + (m.numericId() % 2 == 1 ? MakerspaceRules.ODD_ID_BONUS_HOURS : 0);
    }

    public int unitsAvailable(Equipment e, List<Loan> loans) {
        long out = loans.stream().filter(Loan::active).filter(l -> l.equipmentCode().equalsIgnoreCase(e.code())).count();
        return (int) (e.quantity() - out);
    }

    /** Checks M1, M3 and stock; returns the loan that WOULD be created (not saved). */
    public Loan prepareLoan(String memberId, String equipmentCode, List<Loan> loans, String newLoanId)
            throws RuleViolationException {
        Member m = member(memberId);
        Equipment e = equipment(equipmentCode);
        if (e.needsSafetyQuiz() && !m.safetyQuizPassed()) {
            throw new SafetyQuizRequiredException(e);
        }
        long active = loans.stream().filter(Loan::active).filter(l -> l.memberId().equalsIgnoreCase(m.memberId())).count();
        if (active >= rules.maxActiveLoans()) {
            throw new LoanLimitExceededException(rules.maxActiveLoans());
        }
        if (unitsAvailable(e, loans) <= 0) {
            throw new OutOfStockException(e);
        }
        LocalDateTime now = LocalDateTime.now(clock).withSecond(0).withNano(0);
        return new Loan(newLoanId, m.memberId(), e.code(), now.toString(), now.plusHours(loanHours(m, e)).toString(), null);
    }

    /** M4: late fee if returned at {@code when}. */
    public long lateFeeSen(Loan loan, LocalDateTime when) {
        LocalDateTime due = LocalDateTime.parse(loan.due());
        if (!when.isAfter(due)) {
            return 0;
        }
        long secondsLate = Duration.between(due, when).toSeconds();
        long startedHours = Math.max(1, (secondsLate + 3599) / 3600);   // "per STARTED hour"
        return Math.min(startedHours * rules.lateFeeSenPerHour(), MakerspaceRules.LATE_FEE_CAP_SEN);
    }

    public List<Loan> activeLoansOf(String memberId, List<Loan> loans) {
        List<Loan> out = new ArrayList<>();
        for (Loan l : loans) {
            if (l.active() && l.memberId().equalsIgnoreCase(memberId)) {
                out.add(l);
            }
        }
        return out;
    }

    public List<Member> members() {
        return List.copyOf(members.values());
    }
}
