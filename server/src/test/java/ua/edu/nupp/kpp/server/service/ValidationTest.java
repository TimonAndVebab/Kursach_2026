package ua.edu.nupp.kpp.server.service;

import org.junit.jupiter.api.Test;
import ua.edu.nupp.kpp.server.model.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test void rejectsBlankAuthor() {
        assertThrows(IllegalArgumentException.class, () -> new AuthorService().create(new Author(0,"  ")));
    }
    @Test void rejectsTooLongReviewerName() {
        assertThrows(IllegalArgumentException.class, () -> new ReviewerService().create(new Reviewer(0,"а".repeat(151))));
    }
    @Test void rejectsMissingRegistrationNumber() {
        var doc = new Document(0,"","План",1,45,0);
        assertThrows(IllegalArgumentException.class, () -> new DocumentService().create(doc));
    }
    @Test void rejectsDeadlineBeforeCreation() {
        var date = LocalDate.of(2026,11,18);
        var request = new ApprovalRequest(0,1,1,date,date.minusDays(1),ApprovalStatus.DRAFT);
        assertThrows(IllegalArgumentException.class, () -> new ApprovalRequestService().create(request));
    }
}
