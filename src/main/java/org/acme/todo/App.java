package org.acme.todo;

import java.nio.file.Path;

import javax.swing.SwingUtilities;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;

import lombok.extern.slf4j.Slf4j;

import org.acme.todo.config.AppDirectories;
import org.acme.todo.settings.SettingsService;
import org.acme.todo.ui.MainFrame;
import org.acme.todo.ui.theme.LookAndFeelSupport;

@Slf4j
@SpringBootApplication
public class App implements Runnable {

	private final MainFrame mainFrame;

	public App(@Lazy MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	@Override
	public void run() {
		mainFrame.setVisible(true);
	}

	public static void main(String[] args) {
		Path configDirectory = AppDirectories.resolveConfigDirectory();
		log.info("Resolved config directory: {}", configDirectory.toAbsolutePath());

		// Apply a system-driven baseline theme before Spring creates any beans.
		LookAndFeelSupport.apply("system");

		System.setProperty("spring.config.additional-location", "optional:" + configDirectory.toUri());
		System.setProperty("acme.config.dir", configDirectory.toAbsolutePath().toString());
		log.info("Configured additional Spring config location: {}", configDirectory.toUri());

		ConfigurableApplicationContext context = new SpringApplicationBuilder(App.class).headless(false)
				.registerShutdownHook(true).run(args);

		SettingsService settingsService = context.getBean(SettingsService.class);
		String theme = settingsService.getSettings().theme();
		log.info("Applying theme on startup: {}", theme);
		LookAndFeelSupport.apply(theme);

		App app = context.getBean(App.class);
		SwingUtilities.invokeLater(() -> {
			LookAndFeelSupport.refreshAllWindows();
			log.info("Launching main UI frame");
			app.run();
		});
	}
}
