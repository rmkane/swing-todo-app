package org.acme.todo.todo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

	private final TodoRepository todoRepository;

	public TodoService(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
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
		todoRepository.delete(id);
	}

	@Transactional
	public void deleteAll() {
		todoRepository.deleteAll();
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
}
