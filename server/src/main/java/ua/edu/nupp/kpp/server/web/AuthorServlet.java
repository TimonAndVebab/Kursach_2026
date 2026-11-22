package ua.edu.nupp.kpp.server.web;

import ua.edu.nupp.kpp.server.model.Author;
import ua.edu.nupp.kpp.server.service.AuthorService;
import javax.servlet.http.HttpServletRequest;

public final class AuthorServlet extends EntityServlet<Author> {
    private final AuthorService service = new AuthorService();

    protected AuthorService service() { return service; }

    protected Author form(HttpServletRequest req, int id) {
        return new Author(id, field(req, "fullName"));
    }
}
