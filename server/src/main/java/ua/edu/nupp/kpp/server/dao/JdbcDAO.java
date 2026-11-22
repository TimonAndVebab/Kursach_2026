package ua.edu.nupp.kpp.server.dao;

import ua.edu.nupp.kpp.server.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// SQL для конкретних таблиць лежить у дочірніх DAO.
public abstract class JdbcDAO<T> {
    protected abstract T read(ResultSet row) throws SQLException;

    @FunctionalInterface
    protected interface Parameters {
        void bind(PreparedStatement sql) throws SQLException;
    }

    protected List<T> query(String text, Parameters parameters) throws SQLException {
        try (Connection db = DBUtil.getConnection(); PreparedStatement sql = db.prepareStatement(text)) {
            parameters.bind(sql);
            try (ResultSet rows = sql.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rows.next()) list.add(read(rows));
                return list;
            }
        }
    }

    protected int insert(String text, Parameters parameters) throws SQLException {
        try (Connection db = DBUtil.getConnection();
             PreparedStatement sql = db.prepareStatement(text, Statement.RETURN_GENERATED_KEYS)) {
            parameters.bind(sql);
            sql.executeUpdate();
            // ID бере MySQL з AUTO_INCREMENT. Читаємо ключ саме цього INSERT, бо найбільший ID уже може бути чужим.
            try (ResultSet keys = sql.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("База не повернула id нового запису");
            }
        }
    }

    protected boolean change(String text, Parameters parameters) throws SQLException {
        try (Connection db = DBUtil.getConnection(); PreparedStatement sql = db.prepareStatement(text)) {
            parameters.bind(sql);
            return sql.executeUpdate() == 1;
        }
    }

    public abstract int create(T item) throws SQLException;
    public abstract Optional<T> findById(int id) throws SQLException;
    public abstract List<T> findAll() throws SQLException;
    public abstract boolean update(T item) throws SQLException;
    public abstract boolean delete(int id) throws SQLException;
}
