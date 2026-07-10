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
		log.info("Database schema initialization complete");
	}
}
