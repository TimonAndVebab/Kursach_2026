package ua.edu.nupp.kpp.server.web;

import ua.edu.nupp.kpp.server.model.ApprovalRequest;
import ua.edu.nupp.kpp.server.service.ApprovalRequestService;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Map;
import ua.edu.nupp.kpp.server.model.ApprovalStatus;

public final class ApprovalRequestServlet extends EntityServlet<ApprovalRequest> {
    private final ApprovalRequestService service = new ApprovalRequestService();

    protected ApprovalRequestService service() { return service; }

    protected ApprovalRequest form(HttpServletRequest req, int id) {
        return new ApprovalRequest(id, number(req, "documentId"), number(req, "reviewerId"),
                date(req, "createdOn"), date(req, "dueOn"), id == 0 && req.getParameter("status") == null ? ApprovalStatus.DRAFT : status(req, "status"));
    }

    private ApprovalStatus status(HttpServletRequest req, String key) {
        try { return ApprovalStatus.valueOf(field(req, key)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Вкажіть стан запиту"); }
    }

    @Override
    protected Object read(HttpServletRequest req) throws SQLException {
        if ("priority".equals(req.getParameter("action")))
            return service.priority(number(req, "id"), date(req, "onDate"));
        return super.read(req);
    }

    @Override
    protected Object write(HttpServletRequest req, String action) throws SQLException {
        if ("transition".equals(action)) {
            // Допустимість переходу вже перевіряє Service з другого етапу.
            service.changeStatus(number(req, "id"), status(req, "state"));
            return Map.of("ok", true);
        }
        return super.write(req, action);
    }
}
