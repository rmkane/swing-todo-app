package org.acme.todo.core.model;

import java.time.Instant;
import java.util.List;

public record Todo(Long id, String description, boolean completed, Instant createdAt, Instant completedAt,
		List<TodoCategory> categories) {
}
