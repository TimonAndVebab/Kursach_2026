package ua.edu.nupp.kpp.server.web;

import ua.edu.nupp.kpp.server.model.Document;
import ua.edu.nupp.kpp.server.service.DocumentService;
import javax.servlet.http.HttpServletRequest;

public final class DocumentServlet extends EntityServlet<Document> {
    private final DocumentService service = new DocumentService();

    protected DocumentService service() { return service; }

    protected Document form(HttpServletRequest req, int id) {
        return new Document(id, field(req, "registrationNumber"), field(req, "title"),
                number(req, "authorId"), number(req, "criticalityScore"), number(req, "dependentProcessCount"));
    }
}
