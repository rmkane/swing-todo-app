package org.acme.todo.core.repository.impl;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import org.acme.todo.core.model.Todo;
import org.acme.todo.core.model.TodoCategory;
import org.acme.todo.core.repository.AbstractJdbcRepository;
import org.acme.todo.core.repository.TodoRepository;

@Repository
@DependsOn("databaseInitializer")
public class JdbcTodoRepository extends AbstractJdbcRepository<Todo, Long> implements TodoRepository {

	public JdbcTodoRepository(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}

	@Override
	public Optional<Todo> findById(Long id) {
		List<Todo> todos = jdbcTemplate.query("""
				SELECT
				    id,
				    description,
				    completed,
				    created_at,
				    completed_at
				FROM todo
				WHERE id = ?
				""",
				(resultSet, rowNumber) -> new Todo(resultSet.getLong("id"), resultSet.getString("description"),
						resultSet.getInt("completed") != 0, Instant.parse(resultSet.getString("created_at")),
						parseInstant(resultSet.getString("completed_at")), List.of()),
				id);

		if (todos.isEmpty()) {
			return Optional.empty();
		}

		Todo todo = todos.getFirst();
		List<TodoCategory> categories = findCategoriesByTodo(todo.id());
		return Optional.of(new Todo(todo.id(), todo.description(), todo.completed(), todo.createdAt(),
				todo.completedAt(), categories));
	}

	@Override
	public List<Todo> findAll() {
		List<Todo> todos = jdbcTemplate.query("""
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
						parseInstant(resultSet.getString("completed_at")), List.of()));

		if (todos.isEmpty()) {
			return todos;
		}

		Map<Long, List<TodoCategory>> categoriesByTodoId = loadCategoriesByTodoId();
		List<Todo> enriched = new ArrayList<>(todos.size());
		for (Todo todo : todos) {
			List<TodoCategory> categories = categoriesByTodoId.getOrDefault(todo.id(), List.of());
			enriched.add(new Todo(todo.id(), todo.description(), todo.completed(), todo.createdAt(), todo.completedAt(),
					categories));
		}

		return enriched;
	}

	@Override
	public Todo insert(String description) {
		return insert(description, Instant.now(), false, null, List.of());
	}

	@Override
	public Todo insert(String description, Instant createdAt, boolean completed, Instant completedAt,
			List<Long> categoryIds) {
		KeyHolder keyHolder = new GeneratedKeyHolder();

		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO todo (
					    description,
					    completed,
					    created_at,
					    completed_at
					)
					VALUES (?, ?, ?, ?)
					""", Statement.RETURN_GENERATED_KEYS);

			statement.setString(1, description);
			statement.setInt(2, completed ? 1 : 0);
			statement.setString(3, createdAt.toString());
			statement.setString(4, completedAt == null ? null : completedAt.toString());
			return statement;
		}, keyHolder);

		long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
		setCategoriesForTodo(id, categoryIds);
		List<TodoCategory> categories = findCategoriesByTodo(id);
		return new Todo(id, description, completed, createdAt, completedAt, categories);
	}

	@Override
	public void updateCompleted(long id, boolean completed) {
		String completedAt = completed ? Instant.now().toString() : null;
		int updated = jdbcTemplate.update("""
				UPDATE todo
				SET
				    completed = ?,
				    completed_at = ?
				WHERE id = ?
				""", completed ? 1 : 0, completedAt, id);
		requireAffectedRows(updated, "Todo does not exist: " + id);
	}

	@Override
	public void updateDescription(long id, String description) {
		int updated = jdbcTemplate.update("""
				UPDATE todo
				SET description = ?
				WHERE id = ?
				""", description, id);
		requireAffectedRows(updated, "Todo does not exist: " + id);
	}

	@Override
	public void deleteById(Long id) {
		jdbcTemplate.update("DELETE FROM todo WHERE id = ?", id);
	}

	@Override
	public void deleteAll() {
		jdbcTemplate.update("DELETE FROM todo");
	}

	@Override
	public void setCategoriesForTodo(long todoId, List<Long> categoryIds) {
		jdbcTemplate.update("DELETE FROM todo_category WHERE todo_id = ?", todoId);
		for (Long categoryId : categoryIds) {
			if (categoryId == null) {
				continue;
			}
			jdbcTemplate.update("INSERT INTO todo_category (todo_id, category_id) VALUES (?, ?)", todoId, categoryId);
		}
	}

	@Override
	public List<TodoCategory> findCategoriesByTodo(long todoId) {
		return jdbcTemplate.query("""
				SELECT c.id, c.name, c.color
				FROM category c
				JOIN todo_category tc ON tc.category_id = c.id
				WHERE tc.todo_id = ?
				ORDER BY c.name ASC
				""", (resultSet, rowNumber) -> new TodoCategory(resultSet.getLong("id"), resultSet.getString("name"),
				resultSet.getString("color")), todoId);
	}

	private Map<Long, List<TodoCategory>> loadCategoriesByTodoId() {
		Map<Long, List<TodoCategory>> categoriesByTodoId = new HashMap<>();
		jdbcTemplate.query("""
				SELECT tc.todo_id, c.id, c.name, c.color
				FROM todo_category tc
				JOIN category c ON c.id = tc.category_id
				ORDER BY c.name ASC
				""", resultSet -> {
			long todoId = resultSet.getLong("todo_id");
			TodoCategory category = new TodoCategory(resultSet.getLong("id"), resultSet.getString("name"),
					resultSet.getString("color"));
			categoriesByTodoId.computeIfAbsent(todoId, ignored -> new ArrayList<>()).add(category);
		});
		return categoriesByTodoId;
	}

	private Instant parseInstant(String value) {
		return value == null ? null : Instant.parse(value);
	}
}
