package ua.edu.nupp.kpp.server.service;

import org.junit.jupiter.api.Test;
import ua.edu.nupp.kpp.server.dao.ApprovalRequestDAO;
import ua.edu.nupp.kpp.server.model.*;
import static ua.edu.nupp.kpp.server.model.ApprovalStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;
import java.util.Optional;

class ApprovalRequestServiceTest {
    // Замість MySQL зберігаємо стан одного запиту. Правила переходів виконує справжній Service.
    private ApprovalStatus state = DRAFT;
    private final ApprovalRequestService service = new ApprovalRequestService(new ApprovalRequestDAO() {
        public Optional<ApprovalRequest> findById(int id) {
            return Optional.of(new ApprovalRequest(id,1,1,null,null,state));
        }
        public boolean changeStatus(int id, ApprovalStatus expected, ApprovalStatus next) {
            if (state != expected) return false;
            state = next;
            return true;
        }
    });

    @Test void followsFullApprovalSequence() throws SQLException {
        for (var next : new ApprovalStatus[]{SUBMITTED, IN_REVIEW, APPROVED}) {
            service.changeStatus(1,next);
            assertEquals(next,state);
        }
    }
    @Test void cannotSkipReview() {
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(1,APPROVED));
        assertEquals(DRAFT,state);
    }
    @Test void cannotReopenApprovedRequest() {
        state = APPROVED;
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(1,IN_REVIEW));
        assertEquals(APPROVED,state);
    }
}
