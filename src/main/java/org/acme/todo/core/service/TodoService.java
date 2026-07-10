package org.acme.todo.core.service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.acme.todo.core.model.Todo;
import org.acme.todo.core.model.TodoCategory;
import org.acme.todo.core.repository.TodoCategoryRepository;
import org.acme.todo.core.repository.TodoRepository;

@Service
public class TodoService {

	private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

	private final TodoRepository todoRepository;
	private final TodoCategoryRepository todoCategoryRepository;

	public TodoService(TodoRepository todoRepository, TodoCategoryRepository todoCategoryRepository) {
		this.todoRepository = todoRepository;
		this.todoCategoryRepository = todoCategoryRepository;
	}

	@Transactional(readOnly = true)
	public List<Todo> findAll() {
		return todoRepository.findAll();
	}

	@Transactional
	public Todo add(String description) {
		String normalizedDescription = normalizeDescription(description);
		return todoRepository.insert(normalizedDescription);
	}

	@Transactional
	public void setCompleted(long id, boolean completed) {
		todoRepository.updateCompleted(id, completed);
	}

	@Transactional
	public void rename(long id, String description) {
		todoRepository.updateDescription(id, normalizeDescription(description));
	}

	@Transactional
	public void delete(long id) {
		todoRepository.deleteById(id);
	}

	@Transactional
	public void deleteAll() {
		todoRepository.deleteAll();
	}

	@Transactional(readOnly = true)
	public List<TodoCategory> findCategories() {
		return todoCategoryRepository.findAll();
	}

	@Transactional
	public TodoCategory addCategory(String name, String color) {
		String normalizedName = normalizeCategoryName(name);
		String normalizedColor = normalizeColor(color);

		if (todoCategoryRepository.findByName(normalizedName).isPresent()) {
			throw new IllegalArgumentException("Category already exists: " + normalizedName);
		}

		return todoCategoryRepository.insert(normalizedName, normalizedColor);
	}

	@Transactional
	public TodoCategory findOrCreateCategory(String name, String color) {
		String normalizedName = normalizeCategoryName(name);
		String normalizedColor = normalizeColor(color);

		return todoCategoryRepository.findByName(normalizedName)
				.orElseGet(() -> todoCategoryRepository.insert(normalizedName, normalizedColor));
	}

	@Transactional
	public void updateCategory(long categoryId, String name, String color) {
		String normalizedName = normalizeCategoryName(name);
		String normalizedColor = normalizeColor(color);

		if (todoCategoryRepository.findById(categoryId).isEmpty()) {
			throw new IllegalArgumentException("Category does not exist: " + categoryId);
		}

		TodoCategory existing = todoCategoryRepository.findByName(normalizedName).orElse(null);
		if (existing != null && !existing.id().equals(categoryId)) {
			throw new IllegalArgumentException("Category already exists: " + normalizedName);
		}

		todoCategoryRepository.update(categoryId, normalizedName, normalizedColor);
	}

	@Transactional
	public void deleteCategory(long categoryId) {
		todoCategoryRepository.deleteById(categoryId);
	}

	@Transactional
	public void deleteAllCategories() {
		todoCategoryRepository.deleteAll();
	}

	@Transactional
	public Todo addWithMetadata(String description, boolean completed, Instant createdAt, Instant completedAt,
			List<Long> categoryIds) {
		String normalizedDescription = normalizeDescription(description);
		Instant effectiveCreatedAt = createdAt == null ? Instant.now() : createdAt;
		Instant effectiveCompletedAt = completed ? (completedAt == null ? Instant.now() : completedAt) : null;

		return todoRepository.insert(normalizedDescription, effectiveCreatedAt, completed, effectiveCompletedAt,
				categoryIds == null ? List.of() : categoryIds);
	}

	@Transactional
	public void setCategories(long todoId, List<Long> categoryIds) {
		todoRepository.setCategoriesForTodo(todoId, categoryIds == null ? List.of() : categoryIds);
	}

	private String normalizeDescription(String description) {
		if (description == null) {
			throw new IllegalArgumentException("Description is required");
		}

		String normalized = description.trim();

		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Description cannot be blank");
		}

		if (normalized.length() > 500) {
			throw new IllegalArgumentException("Description cannot exceed 500 characters");
		}

		return normalized;
	}

	private String normalizeCategoryName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Category name is required");
		}

		String normalized = name.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Category name cannot be blank");
		}

		if (normalized.length() > 40) {
			throw new IllegalArgumentException("Category name cannot exceed 40 characters");
		}

		return normalized;
	}

	private String normalizeColor(String color) {
		if (color == null) {
			throw new IllegalArgumentException("Category color is required");
		}

		String normalized = color.trim();
		if (!HEX_COLOR_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("Category color must be a hex value like #3366CC");
		}

		return normalized.toUpperCase();
	}
}
