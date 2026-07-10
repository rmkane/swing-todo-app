package org.acme.todo.config;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DatabaseConfiguration {

	@Bean
	public DataSource dataSource(Path configDirectory, AppProperties appProperties) {
		Path databaseFile = configDirectory.resolve(appProperties.getDatabase().getFilename());
		log.info("Configuring SQLite datasource at {}", databaseFile.toAbsolutePath());

		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		sqliteConfig.setBusyTimeout(5_000);
		sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
		sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

		SQLiteDataSource dataSource = new SQLiteDataSource(sqliteConfig);
		dataSource.setUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
		log.debug("SQLite JDBC URL: {}", dataSource.getUrl());

		return dataSource;
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
}
