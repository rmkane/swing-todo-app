package org.acme.todo.settings;

public record AppSettings(String theme, boolean confirmDelete, boolean showCompleted, int windowWidth,
		int windowHeight) {
}
