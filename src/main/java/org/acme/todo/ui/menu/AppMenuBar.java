package org.acme.todo.ui.menu;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.acme.todo.ui.SettingsDialog;
import org.acme.todo.ui.TodoPanel;
import org.acme.todo.ui.dialog.AboutDialog;
import org.acme.todo.ui.file.TodoFileActions;
import org.acme.todo.ui.menu.builder.MenuBarBuilder;

@Slf4j
@Lazy
@Component
public class AppMenuBar {

	private final TodoFileActions todoFileActions;
	private final SettingsDialog settingsDialog;
	private final AboutDialog aboutDialog;
	private final TodoPanel todoPanel;

	public AppMenuBar(TodoFileActions todoFileActions, SettingsDialog settingsDialog, AboutDialog aboutDialog,
			TodoPanel todoPanel) {
		this.todoFileActions = todoFileActions;
		this.settingsDialog = settingsDialog;
		this.aboutDialog = aboutDialog;
		this.todoPanel = todoPanel;
	}

	public void install(JFrame frame) {
		JMenuBar menuBar = MenuBarBuilder.create().menu("File", file -> file.mnemonic('F')
				.item("Import Todos...",
						item -> item.mnemonic('I').onClick(() -> todoFileActions.importTodos(frame, todoPanel)))
				.item("Export Todos...", item -> item.mnemonic('E').onClick(() -> todoFileActions.exportTodos(frame)))
				.separator().item("Preferences...", item -> item.mnemonic('P').onClick(() -> openPreferences(frame)))
				.separator().item("Exit",
						item -> item.mnemonic('X').onClick(
								() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)))))
				.menu("Help", help -> help.mnemonic('H').item("About",
						item -> item.mnemonic('A').onClick(() -> openAbout(frame))))
				.build();

		frame.setJMenuBar(menuBar);
	}

	private void openPreferences(JFrame frame) {
		if (settingsDialog.open(frame)) {
			todoPanel.reloadTodos();
		}
	}

	private void openAbout(JFrame frame) {
		try {
			aboutDialog.open(frame);
		} catch (Throwable throwable) {
			log.error("Failed to open About dialog", throwable);
			JOptionPane.showMessageDialog(frame,
					"Could not open About dialog. Please rebuild the project and retry.\n\n" + throwable.getMessage(),
					"About unavailable", JOptionPane.ERROR_MESSAGE);
		}
	}
}
