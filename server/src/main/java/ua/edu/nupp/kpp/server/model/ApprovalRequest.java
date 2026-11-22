package ua.edu.nupp.kpp.server.model;

import java.time.LocalDate;

public record ApprovalRequest(
        int id,
        int documentId,
        int reviewerId,
        LocalDate createdOn,
        // Строк свій у кожного рецензента, бо зі спільною датою потім хрін розбереш, хто прострочив.
        LocalDate dueOn,
        ApprovalStatus status) {
}
