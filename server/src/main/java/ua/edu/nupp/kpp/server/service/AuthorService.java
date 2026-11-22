package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.*;
import ua.edu.nupp.kpp.server.model.*;
import java.sql.SQLException;

public class AuthorService extends EntityService<Author> {

    public AuthorService() {
        super(new AuthorDAO());
    }

    @Override
    protected int idOf(Author item) { return item.id(); }

    @Override
    protected void validate(Author item) throws SQLException {
        text(item.fullName(), 150, "ПІБ автора");
    }
}
