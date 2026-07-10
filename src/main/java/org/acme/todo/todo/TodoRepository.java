package org.acme.todo.todo;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@DependsOn("databaseInitializer")
public class TodoRepository {

	private final JdbcTemplate jdbcTemplate;

	public TodoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Todo> findAll() {
		return jdbcTemplate.query("""
				SELECT
				    id,
				    description,
				    completed,
				    created_at,
				    completed_at
				FROM todo
				ORDER BY completed ASC, created_at DESC
				""",
				(resultSet, rowNumber) -> new Todo(resultSet.getLong("id"), resultSet.getString("description"),
						resultSet.getInt("completed") != 0, Instant.parse(resultSet.getString("created_at")),
						parseInstant(resultSet.getString("completed_at"))));
	}

	public Todo insert(String description) {
		Instant createdAt = Instant.now();
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO todo (
					    description,
					    completed,
					    created_at,
					    completed_at
					)
					VALUES (?, 0, ?, NULL)
					""", Statement.RETURN_GENERATED_KEYS);

			statement.setString(1, description);
			statement.setString(2, createdAt.toString());

			return statement;
		}, keyHolder);

		long id = Objects.requireNonNull(keyHolder.getKey()).longValue();

		return new Todo(id, description, false, createdAt, null);
	}

	public void updateCompleted(long id, boolean completed) {
		String completedAt = completed ? Instant.now().toString() : null;

		int updated = jdbcTemplate.update("""
				UPDATE todo
				SET
				    completed = ?,
				    completed_at = ?
				WHERE id = ?
				""", completed ? 1 : 0, completedAt, id);

		requireUpdatedRow(id, updated);
	}

	public void updateDescription(long id, String description) {
		int updated = jdbcTemplate.update("""
				UPDATE todo
				SET description = ?
				WHERE id = ?
				""", description, id);

		requireUpdatedRow(id, updated);
	}

	public void delete(long id) {
		jdbcTemplate.update("DELETE FROM todo WHERE id = ?", id);
	}

	public void deleteAll() {
		jdbcTemplate.update("DELETE FROM todo");
	}

	private Instant parseInstant(String value) {
		return value == null ? null : Instant.parse(value);
	}

	private void requireUpdatedRow(long id, int updated) {
		if (updated == 0) {
			throw new IllegalArgumentException("Todo does not exist: " + id);
		}
	}
}
