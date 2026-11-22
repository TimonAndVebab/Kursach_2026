package ua.edu.nupp.kpp.client.model;

import java.time.LocalDate;

public record ApprovalRequest(
        int id,
        int documentId,
        int reviewerId,
        LocalDate createdOn,
        LocalDate dueOn,
        ApprovalStatus status) {
}
