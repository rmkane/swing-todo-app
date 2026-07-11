package org.acme.todo.ui.theme;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.formdev.flatlaf.util.SystemInfo;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SystemTheme {

	public static boolean isOsDarkTheme() {
		if (SystemInfo.isMacOS) {
			return "dark".equalsIgnoreCase(readCommandOutput("defaults", "read", "-g", "AppleInterfaceStyle"));
		}

		if (SystemInfo.isWindows) {
			String output = readCommandOutput("reg", "query",
					"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v",
					"AppsUseLightTheme");
			return output != null && output.contains("0x0");
		}

		String gtkTheme = System.getenv("GTK_THEME");
		if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
			return true;
		}

		String colorScheme = System.getenv("COLORSCHEME");
		return colorScheme != null && colorScheme.equalsIgnoreCase("dark");
	}

	private static String readCommandOutput(String... command) {
		try {
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			if (!process.waitFor(2, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return null;
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				return reader.lines().map(String::trim).filter(line -> !line.isEmpty()).findFirst().orElse(null);
			}
		} catch (Exception ignored) {
			return null;
		}
	}
}
