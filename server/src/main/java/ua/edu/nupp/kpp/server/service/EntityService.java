package ua.edu.nupp.kpp.server.service;

import ua.edu.nupp.kpp.server.dao.JdbcDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public abstract class EntityService<T> {
    private final JdbcDAO<T> dao;
    private final Logger log = Logger.getLogger(getClass().getName());

    protected EntityService(JdbcDAO<T> dao) {
        this.dao = dao;
    }

    protected abstract void validate(T item) throws SQLException;
    protected abstract int idOf(T item);

    protected static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }

    protected static void text(String value, int max, String field) {
        require(value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= max,
                field + ": заповніть поле (до " + max + " символів)");
    }

    @FunctionalInterface
    protected interface Operation<R> {
        R run() throws SQLException;
    }

    protected <R> R run(String operation, Operation<R> action, boolean write) throws SQLException {
        try {
            R result = action.run();
            // Пишемо про успіх після виконання дії. Якщо база відмовила, у журнал піде причина помилки.
            if (write) log.info(operation + " — виконано");
            return result;
        } catch (IllegalArgumentException error) {
            log.warning(operation + " — " + error.getMessage());
            throw error;
        } catch (SQLException error) {
            log.warning(operation + " — помилка БД, SQLState=" + error.getSQLState());
            throw error;
        }
    }

    protected void validateCreate(T item) throws SQLException { validate(item); }
    protected void validateUpdate(T item) throws SQLException { validate(item); }

    public int create(T item) throws SQLException {
        return run("Створення", () -> {
            require(item != null, "Немає даних");
            validateCreate(item);
            return dao.create(item);
        }, true);
    }

    public T findById(int id) throws SQLException {
        return run("Читання " + id, () -> dao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Запис не знайдено: " + id)), false);
    }

    public List<T> findAll() throws SQLException {
        return run("Список", dao::findAll, false);
    }

    public void update(T item) throws SQLException {
        run("Редагування", () -> {
            require(item != null, "Немає даних");
            validateUpdate(item);
            require(dao.update(item), "Запис не знайдено: " + idOf(item));
            return null;
        }, true);
    }

    public void delete(int id) throws SQLException {
        run("Видалення " + id, () -> {
            require(dao.delete(id), "Запис не знайдено: " + id);
            return null;
        }, true);
    }
}
