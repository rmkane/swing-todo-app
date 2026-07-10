package org.acme.todo.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.acme.todo.settings.AppSettings;
import org.acme.todo.settings.SettingsService;
import org.acme.todo.ui.menu.AppMenuBar;
import org.acme.todo.ui.support.AppIcons;

@Slf4j
@Lazy
@Component
public class MainFrame extends JFrame {

	private static final int MIN_WINDOW_WIDTH = 400;
	private static final int MIN_WINDOW_HEIGHT = 300;

	private final SettingsService settingsService;
	private final AppMenuBar appMenuBar;

	public MainFrame(TodoPanel todoPanel, SettingsService settingsService, AppMenuBar appMenuBar) {
		this.settingsService = settingsService;
		this.appMenuBar = appMenuBar;

		configureFrame(todoPanel);
	}

	private void configureFrame(TodoPanel todoPanel) {
		AppSettings settings = settingsService.getSettings();
		int width = Math.max(MIN_WINDOW_WIDTH, settings.windowWidth());
		int height = Math.max(MIN_WINDOW_HEIGHT, settings.windowHeight());

		setTitle("ACME Todo");
		setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
		setSize(width, height);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		AppIcons.install(this);

		setLayout(new BorderLayout());
		add(todoPanel, BorderLayout.CENTER);
		appMenuBar.install(this);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				try {
					log.debug("Window closing; persisting size {}x{}", getWidth(), getHeight());
					saveWindowSize();
				} catch (RuntimeException exception) {
					log.warn("Could not persist window size: {}", exception.getMessage(), exception);
				}
			}
		});
	}

	private void saveWindowSize() {
		AppSettings current = settingsService.getSettings();
		int width = Math.max(MIN_WINDOW_WIDTH, getWidth());
		int height = Math.max(MIN_WINDOW_HEIGHT, getHeight());

		AppSettings updated = new AppSettings(current.theme(), current.confirmDelete(), current.showCompleted(), width,
				height);

		settingsService.save(updated);
	}
}
