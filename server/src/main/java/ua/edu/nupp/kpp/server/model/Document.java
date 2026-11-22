package ua.edu.nupp.kpp.server.model;

public record Document(
        int id,
        String registrationNumber,
        String title,
        int authorId,
        int criticalityScore,
        // Тут просто кількість: три процеси — це ще не три бали F3.
        int dependentProcessCount) {
}
