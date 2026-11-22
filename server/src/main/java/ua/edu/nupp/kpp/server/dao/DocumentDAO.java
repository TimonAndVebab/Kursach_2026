package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Document;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class DocumentDAO extends JdbcDAO<Document> {
    private static final String SELECT = "SELECT id, registration_number, title, author_id, criticality_score, dependent_process_count FROM document";

    protected Document read(ResultSet row) throws SQLException {
        return new Document(row.getInt("id"),
                row.getString("registration_number"),
                row.getString("title"),
                row.getInt("author_id"),
                row.getInt("criticality_score"),
                row.getInt("dependent_process_count"));
    }

    private void bind(PreparedStatement sql, Document item) throws SQLException {
        sql.setString(1, item.registrationNumber());
        sql.setString(2, item.title());
        sql.setInt(3, item.authorId());
        sql.setInt(4, item.criticalityScore());
        sql.setInt(5, item.dependentProcessCount());
    }

    public int create(Document item) throws SQLException {
        return insert("INSERT INTO document (registration_number, title, author_id, criticality_score, dependent_process_count) VALUES (?, ?, ?, ?, ?)", sql -> {
            bind(sql, item);
        });
    }

    public Optional<Document> findById(int id) throws SQLException {
        return query(SELECT + " WHERE id = ?", sql -> sql.setInt(1, id)).stream().findFirst();
    }

    public List<Document> findAll() throws SQLException {
        return query(SELECT + " ORDER BY id", sql -> {});
    }

    public boolean update(Document item) throws SQLException {
        return change("UPDATE document SET registration_number = ?, title = ?, author_id = ?, criticality_score = ?, dependent_process_count = ? WHERE id = ?", sql -> {
            bind(sql, item);
            sql.setInt(6, item.id());
        });
    }

    public boolean delete(int id) throws SQLException {
        return change("DELETE FROM document WHERE id = ?", sql -> sql.setInt(1, id));
    }
}
