package ua.edu.nupp.kpp.server.model;

// У базі ці назви зберігаються дослівно: DRAFT — чернетка, IN_REVIEW — на розгляді.
public enum ApprovalStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    APPROVED
}
