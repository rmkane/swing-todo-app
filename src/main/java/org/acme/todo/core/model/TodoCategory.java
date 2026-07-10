package org.acme.todo.core.model;

public record TodoCategory(Long id, String name, String color) {

	@Override
	public String toString() {
		return name;
	}
}
