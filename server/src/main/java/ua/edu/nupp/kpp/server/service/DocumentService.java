package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.*;
import ua.edu.nupp.kpp.server.model.*;
import java.sql.SQLException;

public class DocumentService extends EntityService<Document> {
    private final AuthorDAO authors = new AuthorDAO();

    public DocumentService() {
        super(new DocumentDAO());
    }

    @Override
    protected int idOf(Document item) { return item.id(); }

    @Override
    protected void validate(Document item) throws SQLException {
        text(item.registrationNumber(), 40, "Реєстраційний номер");
        text(item.title(), 200, "Назва");
        require(item.criticalityScore() >= 0 && item.criticalityScore() <= 100, "Критичність поза межами 0–100");
        require(item.dependentProcessCount() >= 0, "Кількість залежних процесів від’ємна");
        // Перевіряємо автора тут, щоб користувач отримав зрозуміле повідомлення ще до запису документа.
        require(authors.findById(item.authorId()).isPresent(), "Автора не знайдено");
    }
}
