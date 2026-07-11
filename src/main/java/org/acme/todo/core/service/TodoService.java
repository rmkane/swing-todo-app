package org.acme.todo.core.service;

import java.time.Instant;
import java.util.List;

import org.acme.todo.core.model.Todo;
import org.acme.todo.core.model.TodoCategory;

public interface TodoService {

	List<Todo> findAll();

	Todo add(String description);

	void setCompleted(long id, boolean completed);

	void rename(long id, String description);

	void delete(long id);

	void deleteAll();

	List<TodoCategory> findCategories();

	TodoCategory addCategory(String name, String color);

	TodoCategory findOrCreateCategory(String name, String color);

	void updateCategory(long categoryId, String name, String color);

	void deleteCategory(long categoryId);

	void deleteAllCategories();

	Todo addWithMetadata(String description, boolean completed, Instant createdAt, Instant completedAt,
			List<Long> categoryIds);

	void setCategories(long todoId, List<Long> categoryIds);
}
