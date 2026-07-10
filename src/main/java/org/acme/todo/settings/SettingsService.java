package org.acme.todo.settings;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;

import org.acme.todo.config.AppProperties;

@Slf4j
@Service
public class SettingsService {

	private final AppProperties appProperties;
	private final Path settingsFile;
	private final Yaml yaml;

	public SettingsService(AppProperties appProperties, Path configDirectory) {
		this.appProperties = appProperties;
		this.settingsFile = configDirectory.resolve("application.yml");
		this.yaml = createYaml();
		log.info("Settings file path: {}", settingsFile.toAbsolutePath());
	}

	public AppSettings getSettings() {
		AppProperties.Settings settings = appProperties.getSettings();

		AppSettings resolved = new AppSettings(settings.getTheme(), settings.isConfirmDelete(),
				settings.isShowCompleted(), settings.getWindowWidth(), settings.getWindowHeight());
		log.debug("Loaded settings: theme={}, confirmDelete={}, showCompleted={}, size={}x{}", resolved.theme(),
				resolved.confirmDelete(), resolved.showCompleted(), resolved.windowWidth(), resolved.windowHeight());
		return resolved;
	}

	public void save(AppSettings settings) {
		log.info("Saving settings: theme={}, confirmDelete={}, showCompleted={}, size={}x{}", settings.theme(),
				settings.confirmDelete(), settings.showCompleted(), settings.windowWidth(), settings.windowHeight());
		validate(settings);
		updateRuntimeSettings(settings);

		Map<String, Object> document = createDocument(settings);
		writeAtomically(document);
	}

	public Path getSettingsFile() {
		return settingsFile;
	}

	private void updateRuntimeSettings(AppSettings newSettings) {
		AppProperties.Settings current = appProperties.getSettings();

		current.setTheme(newSettings.theme());
		current.setConfirmDelete(newSettings.confirmDelete());
		current.setShowCompleted(newSettings.showCompleted());
		current.setWindowWidth(newSettings.windowWidth());
		current.setWindowHeight(newSettings.windowHeight());
	}

	private Map<String, Object> createDocument(AppSettings settings) {
		Map<String, Object> settingsValues = new LinkedHashMap<>();
		settingsValues.put("theme", settings.theme());
		settingsValues.put("confirm-delete", settings.confirmDelete());
		settingsValues.put("show-completed", settings.showCompleted());
		settingsValues.put("window-width", settings.windowWidth());
		settingsValues.put("window-height", settings.windowHeight());

		Map<String, Object> appValues = new LinkedHashMap<>();
		appValues.put("settings", settingsValues);

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("app", appValues);

		return root;
	}

	private void writeAtomically(Map<String, Object> document) {
		Path temporaryFile = settingsFile.resolveSibling(settingsFile.getFileName() + ".tmp");

		try {
			Files.createDirectories(settingsFile.getParent());

			try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
				yaml.dump(document, writer);
			}

			moveTemporaryFile(temporaryFile);
			log.info("Settings saved to {}", settingsFile.toAbsolutePath());
		} catch (IOException exception) {
			log.error("Failed to save settings to {}", settingsFile.toAbsolutePath(), exception);
			throw new IllegalStateException("Could not save settings to " + settingsFile, exception);
		}
	}

	private void moveTemporaryFile(Path temporaryFile) throws IOException {
		try {
			Files.move(temporaryFile, settingsFile, StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException exception) {
			// Some filesystems do not support atomic moves.
			Files.move(temporaryFile, settingsFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void validate(AppSettings settings) {
		String theme = settings.theme();
		if (theme == null || (!theme.equals("system") && !theme.equals("light") && !theme.equals("dark"))) {
			throw new IllegalArgumentException("Theme must be one of: system, light, dark");
		}

		if (settings.windowWidth() < 400) {
			throw new IllegalArgumentException("Window width must be at least 400");
		}

		if (settings.windowHeight() < 300) {
			throw new IllegalArgumentException("Window height must be at least 300");
		}
	}

	private Yaml createYaml() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		options.setIndent(2);

		return new Yaml(options);
	}
}
