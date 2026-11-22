package ua.edu.nupp.kpp.server.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.edu.nupp.kpp.server.service.EntityService;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public abstract class EntityServlet<T> extends HttpServlet {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    protected abstract EntityService<T> service();
    protected abstract T form(HttpServletRequest req, int id);

    protected static String field(HttpServletRequest req, String key) {
        String value = req.getParameter(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Заповніть поле " + key);
        return value.strip();
    }

    protected static int number(HttpServletRequest req, String key) {
        try { return Integer.parseInt(field(req, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Потрібне ціле число у полі " + key); }
    }

    protected static LocalDate date(HttpServletRequest req, String key) {
        try { return LocalDate.parse(field(req, key)); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Формат дати для " + key + ": РРРР-ММ-ДД"); }
    }

    protected Object read(HttpServletRequest req) throws SQLException {
        if (req.getParameter("action") != null) throw new IllegalArgumentException("Невідома дія для читання");
        if (req.getParameter("id") != null) return service().findById(number(req, "id"));
        return service().findAll();
    }

    protected Object write(HttpServletRequest req, String action) throws SQLException {
        return switch (action) {
            case "add" -> Map.of("id", service().create(form(req, 0)));
            case "update" -> {
                service().update(form(req, number(req, "id")));
                yield Map.of("ok", true);
            }
            case "delete" -> {
                service().delete(number(req, "id"));
                yield Map.of("ok", true);
            }
            default -> throw new IllegalArgumentException("Невідома дія");
        };
    }

    protected final void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        respond(req, res, false);
    }

    protected final void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        respond(req, res, true);
    }

    private void respond(HttpServletRequest req, HttpServletResponse res, boolean post) throws IOException {
        req.setCharacterEncoding("UTF-8");
        res.setContentType("application/json;charset=UTF-8");
        Object result;
        try {
            result = post ? write(req, field(req, "action")) : read(req);
        } catch (IllegalArgumentException e) {
            res.setStatus(400);
            result = Map.of("error", e.getMessage());
        } catch (SQLException e) {
            // Дублі номера і видалення пов’язаного запису користувач може виправити у формі.
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                res.setStatus(400);
                result = Map.of("error", "Перевірте унікальність номера та зв’язки запису.");
            } else {
                log("Помилка роботи з БД", e);
                res.setStatus(500);
                result = Map.of("error", "Серверу не вдалося виконати операцію з базою.");
            }
        } catch (RuntimeException e) {
            log("Помилка обробки запиту", e);
            res.setStatus(500);
            result = Map.of("error", "Внутрішня помилка сервера.");
        }
        json.writeValue(res.getOutputStream(), result);
    }
}
