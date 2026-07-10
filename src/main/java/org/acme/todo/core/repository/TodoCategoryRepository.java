package org.acme.todo.core.repository;

import java.util.List;
import java.util.Optional;

import org.acme.todo.core.model.TodoCategory;

public interface TodoCategoryRepository extends CrudRepository<TodoCategory, Long> {

	Optional<TodoCategory> findByName(String name);

	TodoCategory insert(String name, String color);

	void update(long categoryId, String name, String color);

	List<TodoCategory> findByTodoId(long todoId);
}
