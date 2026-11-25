package my.edu.taylors.ap.scenario.makerspace;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceExceptions.NotYourLoanException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Makerspace tools. Note {@code @ToolMemoryId}: LangChain4j passes the CURRENT user's memory id,
 * so the model cannot act on behalf of another member even if asked to (house rule M5).
 */
public class MakerspaceTools {

    private final MakerspaceService service;
    private final JsonStore<Loan> loans;
    private final Clock clock;

    public MakerspaceTools(MakerspaceService service, JsonStore<Loan> loans, Clock clock) {
        this.service = service;
        this.loans = loans;
        this.clock = clock;
    }

    @Tool("Find equipment by keyword, code or category. Shows how many units are free now.")
    public String findEquipment(@P("keyword, code or category, e.g. 'solder' or 'PRINTER3D'") String keyword)
            throws DataAccessException {
        List<Loan> all = loans.findAll();
        List<Equipment> hits = service.find(keyword);
        if (hits.isEmpty()) {
            return "No equipment matches '" + keyword + "'.";
        }
        return hits.stream()
                .map(e -> e.code() + " " + e.name() + " [" + e.category() + "] free: " + service.unitsAvailable(e, all)
                        + "/" + e.quantity() + (e.needsSafetyQuiz() ? " (safety quiz required)" : ""))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Borrow one unit of equipment for the current member. Returns the loan ID and due time.")
    public String borrow(@ToolMemoryId String memberId, @P("equipment code, e.g. EQ-SOL-01") String equipmentCode)
            throws RuleViolationException, DataAccessException {
        List<Loan> all = loans.findAll();
        Loan loan = service.prepareLoan(memberId, equipmentCode, all, String.format("LN-%04d", all.size() + 1));
        loans.append(loan);
        return "Loan " + loan.loanId() + " created for " + loan.equipmentCode() + ". Due back " + loan.due().replace('T', ' ') + ".";
    }

    @Tool("List the current member's active loans with due times.")
    public String myLoans(@ToolMemoryId String memberId) throws RuleViolationException, DataAccessException {
        service.member(memberId);
        List<Loan> mine = service.activeLoansOf(memberId, loans.findAll());
        if (mine.isEmpty()) {
            return "You have no active loans.";
        }
        return mine.stream().map(l -> l.loanId() + " " + l.equipmentCode() + " due " + l.due().replace('T', ' '))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Return a loaned item now and report any late fee.")
    public String returnItem(@ToolMemoryId String memberId, @P("loan ID, e.g. LN-0004") String loanId)
            throws RuleViolationException, DataAccessException {
        List<Loan> all = loans.findAll();
        for (int i = 0; i < all.size(); i++) {
            Loan l = all.get(i);
            if (l.loanId().equalsIgnoreCase(loanId) && l.active()) {
                if (!l.memberId().equalsIgnoreCase(memberId)) {
                    throw new NotYourLoanException(loanId);
                }
                LocalDateTime now = LocalDateTime.now(clock).withSecond(0).withNano(0);
                long fee = service.lateFeeSen(l, now);
                all.set(i, l.returned(now.toString()));
                loans.saveAll(all);
                return "Returned " + l.equipmentCode() + ". Late fee: " + Money.rm(fee) + ".";
            }
        }
        return "No active loan with ID " + loanId + ".";
    }
}
