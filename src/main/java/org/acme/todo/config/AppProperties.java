package org.acme.todo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private final Database database = new Database();
	private final Settings settings = new Settings();

	public Database getDatabase() {
		return database;
	}

	public Settings getSettings() {
		return settings;
	}

	public static class Database {

		private String filename = "todo.db";

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}
	}

	public static class Settings {

		private String theme = "system";
		private boolean confirmDelete = true;
		private boolean showCompleted = true;
		private int windowWidth = 800;
		private int windowHeight = 600;

		public String getTheme() {
			return theme;
		}

		public void setTheme(String theme) {
			this.theme = theme;
		}

		public boolean isConfirmDelete() {
			return confirmDelete;
		}

		public void setConfirmDelete(boolean confirmDelete) {
			this.confirmDelete = confirmDelete;
		}

		public boolean isShowCompleted() {
			return showCompleted;
		}

		public void setShowCompleted(boolean showCompleted) {
			this.showCompleted = showCompleted;
		}

		public int getWindowWidth() {
			return windowWidth;
		}

		public void setWindowWidth(int windowWidth) {
			this.windowWidth = windowWidth;
		}

		public int getWindowHeight() {
			return windowHeight;
		}

		public void setWindowHeight(int windowHeight) {
			this.windowHeight = windowHeight;
		}
	}
}
