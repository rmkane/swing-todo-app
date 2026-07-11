package org.acme.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private final Database database = new Database();
	private final Settings settings = new Settings();

	@Data
	public static class Database {

		private String filename = "todo.db";
	}

	@Data
	public static class Settings {

		private String theme = "system";
		private boolean confirmDelete = true;
		private boolean showCompleted = true;
		private int windowWidth = 800;
		private int windowHeight = 600;
	}
}
