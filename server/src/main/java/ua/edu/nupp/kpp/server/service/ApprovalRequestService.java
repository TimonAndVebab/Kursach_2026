package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.*;
import ua.edu.nupp.kpp.server.model.*;
import java.sql.SQLException;
import java.time.LocalDate;

public class ApprovalRequestService extends EntityService<ApprovalRequest> {
    private final DocumentDAO documents = new DocumentDAO();
    private final ReviewerDAO reviewers = new ReviewerDAO();
    private final ApprovalRequestDAO requests;

    ApprovalRequestService(ApprovalRequestDAO requests) {
        super(requests);
        this.requests = requests;
    }

    public ApprovalRequestService() {
        this(new ApprovalRequestDAO());
    }

    @Override
    protected int idOf(ApprovalRequest item) { return item.id(); }

    @Override
    protected void validate(ApprovalRequest item) throws SQLException {
        require(item.createdOn() != null && item.dueOn() != null, "Потрібні обидві дати");
        require(!item.dueOn().isBefore(item.createdOn()), "Строк раніше дати створення");
        require(documents.findById(item.documentId()).isPresent(), "Документ не знайдено");
        require(reviewers.findById(item.reviewerId()).isPresent(), "Рецензента не знайдено");
    }

    @Override
    protected void validateCreate(ApprovalRequest item) throws SQLException {
        validate(item);
        require(item.status() == ApprovalStatus.DRAFT, "Новий запит починається з чернетки");
    }

    @Override
    protected void validateUpdate(ApprovalRequest item) throws SQLException {
        validate(item);
        require(findById(item.id()).status() == item.status(), "Для зміни стану викличте changeStatus");
    }

    public void changeStatus(int id, ApprovalStatus next) throws SQLException {
        run("Зміна стану запиту " + id + " на " + next, () -> {
            // Беремо поточний стан із бази: поки форма була відкрита, хтось уже міг подати цей запит.
            ApprovalStatus current = findById(id).status();
            boolean allowed = switch (current) {
                case DRAFT -> next == ApprovalStatus.SUBMITTED;
                case SUBMITTED -> next == ApprovalStatus.IN_REVIEW;
                case IN_REVIEW -> next == ApprovalStatus.APPROVED;
                case APPROVED -> false;
            };
            require(allowed, "Заборонений перехід: " + current + " → " + next);
            require(requests.changeStatus(id, current, next), "Стан уже змінився. Оновіть запит");
            return null;
        }, true);
    }

    public PriorityCalculator.Score priority(int id, LocalDate onDate) throws SQLException {
        return run("Розрахунок показника " + id, () -> {
            ApprovalRequest request = findById(id);
            Document doc = documents.findById(request.documentId())
                    .orElseThrow(() -> new IllegalArgumentException("Документ не знайдено"));
            // У двох рецензентів одного документа критичність спільна, а строк кожен має свій.
            return PriorityCalculator.calculate(request.dueOn(), onDate, doc.criticalityScore(), doc.dependentProcessCount());
        }, false);
    }
}
