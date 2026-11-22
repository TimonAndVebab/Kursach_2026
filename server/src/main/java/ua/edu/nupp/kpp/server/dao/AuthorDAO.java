package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.model.Author;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class AuthorDAO extends JdbcDAO<Author> {
    private static final String SELECT = "SELECT id, full_name FROM author";

    protected Author read(ResultSet row) throws SQLException {
        return new Author(row.getInt("id"),
                row.getString("full_name"));
    }

    private void bind(PreparedStatement sql, Author item) throws SQLException {
        sql.setString(1, item.fullName());
    }

    public int create(Author item) throws SQLException {
        return insert("INSERT INTO author (full_name) VALUES (?)", sql -> {
            bind(sql, item);
        });
    }

    public Optional<Author> findById(int id) throws SQLException {
        return query(SELECT + " WHERE id = ?", sql -> sql.setInt(1, id)).stream().findFirst();
    }

    public List<Author> findAll() throws SQLException {
        return query(SELECT + " ORDER BY id", sql -> {});
    }

    public boolean update(Author item) throws SQLException {
        return change("UPDATE author SET full_name = ? WHERE id = ?", sql -> {
            bind(sql, item);
            sql.setInt(2, item.id());
        });
    }

    public boolean delete(int id) throws SQLException {
        return change("DELETE FROM author WHERE id = ?", sql -> sql.setInt(1, id));
    }
}
