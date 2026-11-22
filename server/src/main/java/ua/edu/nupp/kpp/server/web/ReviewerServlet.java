package ua.edu.nupp.kpp.server.web;

import ua.edu.nupp.kpp.server.model.Reviewer;
import ua.edu.nupp.kpp.server.service.ReviewerService;
import javax.servlet.http.HttpServletRequest;

public final class ReviewerServlet extends EntityServlet<Reviewer> {
    private final ReviewerService service = new ReviewerService();

    protected ReviewerService service() { return service; }

    protected Reviewer form(HttpServletRequest req, int id) {
        return new Reviewer(id, field(req, "fullName"));
    }
}
