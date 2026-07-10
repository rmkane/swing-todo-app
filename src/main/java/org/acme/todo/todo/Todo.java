package org.acme.todo.todo;

import java.time.Instant;

public record Todo(Long id, String description, boolean completed, Instant createdAt, Instant completedAt) {
}
