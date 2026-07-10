package org.acme.todo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AppDirectories {

	private static final String APP_DIRECTORY_NAME = "ACME Todo";

	private AppDirectories() {
	}

	public static Path resolveConfigDirectory() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

		Path directory;

		if (osName.contains("win")) {
			directory = resolveWindowsDirectory();
		} else if (osName.contains("mac")) {
			directory = resolveMacDirectory();
		} else {
			directory = resolveUnixDirectory();
		}

		return createDirectory(directory);
	}

	private static Path resolveWindowsDirectory() {
		String appData = System.getenv("APPDATA");

		if (appData != null && !appData.isBlank()) {
			return Path.of(appData, APP_DIRECTORY_NAME);
		}

		return Path.of(System.getProperty("user.home"), "AppData", "Roaming", APP_DIRECTORY_NAME);
	}

	private static Path resolveMacDirectory() {
		return Path.of(System.getProperty("user.home"), "Library", "Application Support", APP_DIRECTORY_NAME);
	}

	private static Path resolveUnixDirectory() {
		String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");

		if (xdgConfigHome != null && !xdgConfigHome.isBlank()) {
			return Path.of(xdgConfigHome, "acme-todo");
		}

		return Path.of(System.getProperty("user.home"), ".config", "acme-todo");
	}

	private static Path createDirectory(Path directory) {
		try {
			return Files.createDirectories(directory);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not create application directory: " + directory, exception);
		}
	}
}
