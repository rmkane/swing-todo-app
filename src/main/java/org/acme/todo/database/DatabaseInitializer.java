package org.acme.todo.database;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DatabaseInitializer {

	private final DataSource dataSource;
	private final Resource migrationScript;

	public DatabaseInitializer(DataSource dataSource, @Value("classpath:db/migration.sql") Resource migrationScript) {
		this.dataSource = dataSource;
		this.migrationScript = migrationScript;
	}

	@PostConstruct
	public void initialize() {
		log.info("Initializing database schema");

		var populator = new ResourceDatabasePopulator(migrationScript);
		populator.execute(dataSource);

		log.info("Database schema initialization complete");
	}
}
