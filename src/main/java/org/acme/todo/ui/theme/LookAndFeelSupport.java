package org.acme.todo.ui.theme;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LookAndFeelSupport {

	public static void apply(String themeName) {
		String normalized = normalizeTheme(themeName);

		switch (normalized) {
			case "light" -> FlatLightLaf.setup();
			case "dark" -> FlatDarkLaf.setup();
			default -> applySystemTheme();
		}

		showMenuMnemonics();
	}

	public static void refreshAllWindows() {
		FlatLaf.updateUI();
	}

	private static void showMenuMnemonics() {
		UIManager.put("Component.hideMnemonics", false);
	}

	private static void applySystemTheme() {
		if (SystemTheme.isOsDarkTheme()) {
			FlatDarkLaf.setup();
			return;
		}

		FlatLightLaf.setup();
	}

	private static String normalizeTheme(String themeName) {
		if (themeName == null) {
			return "system";
		}

		return themeName.trim().toLowerCase();
	}
}
