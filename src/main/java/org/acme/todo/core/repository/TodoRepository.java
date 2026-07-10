package org.acme.todo.core.repository;

import java.time.Instant;
import java.util.List;

import org.acme.todo.core.model.Todo;
import org.acme.todo.core.model.TodoCategory;

public interface TodoRepository extends CrudRepository<Todo, Long> {

	Todo insert(String description);

	Todo insert(String description, Instant createdAt, boolean completed, Instant completedAt, List<Long> categoryIds);

	void updateCompleted(long id, boolean completed);

	void updateDescription(long id, String description);

	void setCategoriesForTodo(long todoId, List<Long> categoryIds);

	List<TodoCategory> findCategoriesByTodo(long todoId);
}
