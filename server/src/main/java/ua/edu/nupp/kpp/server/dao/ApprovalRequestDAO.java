package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.ApprovalRequest;
import ua.edu.nupp.kpp.server.model.ApprovalStatus;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class ApprovalRequestDAO extends JdbcDAO<ApprovalRequest> {
    private static final String SELECT = "SELECT id, document_id, reviewer_id, created_on, due_on, status FROM approval_request";

    protected ApprovalRequest read(ResultSet row) throws SQLException {
        return new ApprovalRequest(row.getInt("id"),
                row.getInt("document_id"),
                row.getInt("reviewer_id"),
                row.getDate("created_on").toLocalDate(),
                row.getDate("due_on").toLocalDate(),
                ApprovalStatus.valueOf(row.getString("status")));
    }

    private void bind(PreparedStatement sql, ApprovalRequest item) throws SQLException {
        sql.setInt(1, item.documentId());
        sql.setInt(2, item.reviewerId());
        sql.setObject(3, item.createdOn());
        sql.setObject(4, item.dueOn());
    }

    public int create(ApprovalRequest item) throws SQLException {
        return insert("INSERT INTO approval_request (document_id, reviewer_id, created_on, due_on, status) VALUES (?, ?, ?, ?, ?)", sql -> {
            bind(sql, item);
            sql.setString(5, item.status().name());
        });
    }

    public Optional<ApprovalRequest> findById(int id) throws SQLException {
        return query(SELECT + " WHERE id = ?", sql -> sql.setInt(1, id)).stream().findFirst();
    }

    public List<ApprovalRequest> findAll() throws SQLException {
        return query(SELECT + " ORDER BY id", sql -> {});
    }

    public boolean update(ApprovalRequest item) throws SQLException {
        // Тут міняємо поля запиту. Стан має окрему команду, щоб редагуванням строку не перескочити розгляд.
        return change("UPDATE approval_request SET document_id = ?, reviewer_id = ?, created_on = ?, due_on = ? WHERE id = ?", sql -> {
            bind(sql, item);
            sql.setInt(5, item.id());
        });
    }

    public boolean delete(int id) throws SQLException {
        return change("DELETE FROM approval_request WHERE id = ?", sql -> sql.setInt(1, id));
    }

    // Якщо інший запит уже змінив стан, цей UPDATE не зачепить рядок.
    public boolean changeStatus(int id, ApprovalStatus expected, ApprovalStatus next) throws SQLException {
        return change("UPDATE approval_request SET status = ? WHERE id = ? AND status = ?", sql -> {
            sql.setString(1, next.name());
            sql.setInt(2, id);
            sql.setString(3, expected.name());
        });
    }
}
