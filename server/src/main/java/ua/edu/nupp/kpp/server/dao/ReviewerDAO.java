package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Reviewer;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class ReviewerDAO extends JdbcDAO<Reviewer> {
    private static final String SELECT = "SELECT id, full_name FROM reviewer";

    protected Reviewer read(ResultSet row) throws SQLException {
        return new Reviewer(row.getInt("id"),
                row.getString("full_name"));
    }

    private void bind(PreparedStatement sql, Reviewer item) throws SQLException {
        sql.setString(1, item.fullName());
    }

    public int create(Reviewer item) throws SQLException {
        return insert("INSERT INTO reviewer (full_name) VALUES (?)", sql -> {
            bind(sql, item);
        });
    }

    public Optional<Reviewer> findById(int id) throws SQLException {
        return query(SELECT + " WHERE id = ?", sql -> sql.setInt(1, id)).stream().findFirst();
    }

    public List<Reviewer> findAll() throws SQLException {
        return query(SELECT + " ORDER BY id", sql -> {});
    }

    public boolean update(Reviewer item) throws SQLException {
        return change("UPDATE reviewer SET full_name = ? WHERE id = ?", sql -> {
            bind(sql, item);
            sql.setInt(2, item.id());
        });
    }

    public boolean delete(int id) throws SQLException {
        return change("DELETE FROM reviewer WHERE id = ?", sql -> sql.setInt(1, id));
    }
}
