package org.acme.todo.core.model;

import org.springframework.lang.NonNull;

public record TodoCategory(Long id, String name, String color) {

	@NonNull
	@Override
	public String toString() {
		return name;
	}
}
