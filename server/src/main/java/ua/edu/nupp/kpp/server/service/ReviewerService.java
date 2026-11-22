package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.*;
import ua.edu.nupp.kpp.server.model.*;
import java.sql.SQLException;

public class ReviewerService extends EntityService<Reviewer> {

    public ReviewerService() {
        super(new ReviewerDAO());
    }

    @Override
    protected int idOf(Reviewer item) { return item.id(); }

    @Override
    protected void validate(Reviewer item) throws SQLException {
        text(item.fullName(), 150, "ПІБ рецензента");
    }
}
