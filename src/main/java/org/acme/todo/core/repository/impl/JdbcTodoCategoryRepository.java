package org.acme.todo.core.repository.impl;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import org.acme.todo.core.model.TodoCategory;
import org.acme.todo.core.repository.AbstractJdbcRepository;
import org.acme.todo.core.repository.TodoCategoryRepository;

@Repository
@DependsOn("databaseInitializer")
public class JdbcTodoCategoryRepository extends AbstractJdbcRepository<TodoCategory, Long>
		implements
			TodoCategoryRepository {

	public JdbcTodoCategoryRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	@Override
	public Optional<TodoCategory> findById(Long id) {
		List<TodoCategory> categories = jdbcTemplate.query("""
				SELECT id, name, color
				FROM category
				WHERE id = ?
				""", (resultSet, rowNumber) -> new TodoCategory(resultSet.getLong("id"), resultSet.getString("name"),
				resultSet.getString("color")), id);

		return categories.stream().findFirst();
	}

	@Override
	public List<TodoCategory> findAll() {
		return jdbcTemplate.query("""
				SELECT id, name, color
				FROM category
				ORDER BY name ASC
				""", (resultSet, rowNumber) -> new TodoCategory(resultSet.getLong("id"), resultSet.getString("name"),
				resultSet.getString("color")));
	}

	@Override
	public void deleteById(Long categoryId) {
		jdbcTemplate.update("DELETE FROM category WHERE id = ?", categoryId);
	}

	@Override
	public void deleteAll() {
		jdbcTemplate.update("DELETE FROM category");
	}

	@Override
	public Optional<TodoCategory> findByName(String name) {
		List<TodoCategory> categories = jdbcTemplate.query("""
				SELECT id, name, color
				FROM category
				WHERE name = ?
				""", (resultSet, rowNumber) -> new TodoCategory(resultSet.getLong("id"), resultSet.getString("name"),
				resultSet.getString("color")), name);

		return categories.stream().findFirst();
	}

	@Override
	public TodoCategory insert(String name, String color) {
		Instant createdAt = Instant.now();
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO category (
					    name,
					    color,
					    created_at
					)
					VALUES (?, ?, ?)
					""", Statement.RETURN_GENERATED_KEYS);

			statement.setString(1, name);
			statement.setString(2, color);
			statement.setString(3, createdAt.toString());

			return statement;
		}, keyHolder);

		long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
		return new TodoCategory(id, name, color);
	}

	@Override
	public void update(long categoryId, String name, String color) {
		int updated = jdbcTemplate.update("""
				UPDATE category
				SET
				    name = ?,
				    color = ?
				WHERE id = ?
				""", name, color, categoryId);

		requireAffectedRows(updated, "Category does not exist: " + categoryId);
	}

	@Override
	public List<TodoCategory> findByTodoId(long todoId) {
		return jdbcTemplate.query("""
				SELECT c.id, c.name, c.color
				FROM category c
				JOIN todo_category tc ON tc.category_id = c.id
				WHERE tc.todo_id = ?
				ORDER BY c.name ASC
				""", (resultSet, rowNumber) -> new TodoCategory(resultSet.getLong("id"), resultSet.getString("name"),
				resultSet.getString("color")), todoId);
	}
}
