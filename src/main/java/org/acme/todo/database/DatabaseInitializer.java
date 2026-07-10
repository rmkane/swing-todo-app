package org.acme.todo.database;

import jakarta.annotation.PostConstruct;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DatabaseInitializer {

	private final JdbcTemplate jdbcTemplate;

	public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@PostConstruct
	public void initialize() {
		log.info("Initializing database schema");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS todo (
				    id INTEGER PRIMARY KEY AUTOINCREMENT,
				    description TEXT NOT NULL,
				    completed INTEGER NOT NULL DEFAULT 0,
				    created_at TEXT NOT NULL,
				    completed_at TEXT
				)
				""");

		jdbcTemplate.execute("""
				CREATE INDEX IF NOT EXISTS todo_completed_idx
				    ON todo (completed)
				""");

		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS category (
				    id INTEGER PRIMARY KEY AUTOINCREMENT,
				    name TEXT NOT NULL UNIQUE,
				    color TEXT NOT NULL,
				    created_at TEXT NOT NULL
				)
				""");

		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS todo_category (
				    todo_id INTEGER NOT NULL,
				    category_id INTEGER NOT NULL,
				    PRIMARY KEY (todo_id, category_id),
				    FOREIGN KEY (todo_id) REFERENCES todo(id) ON DELETE CASCADE,
				    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
				)
				""");

		jdbcTemplate.execute("""
				CREATE INDEX IF NOT EXISTS todo_category_todo_idx
				    ON todo_category (todo_id)
				""");

		jdbcTemplate.execute("""
				CREATE INDEX IF NOT EXISTS todo_category_category_idx
				    ON todo_category (category_id)
				""");
		log.info("Database schema initialization complete");
	}
}
