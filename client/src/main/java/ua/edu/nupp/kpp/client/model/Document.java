package ua.edu.nupp.kpp.client.model;

public record Document(
        int id,
        String registrationNumber,
        String title,
        int authorId,
        int criticalityScore,
        int dependentProcessCount) {
}
