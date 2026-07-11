package org.acme.todo.ui.support;

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppIcons {

	private static final int[] SIZES = {16, 32, 48, 64, 128, 256};
	private static final int[] DIALOG_ICON_SIZES = {128, 256, 64, 48, 32, 16};
	private static final String ICON_PATH_FORMAT = "/icons/icon-%d.png";

	public static Icon dialogIcon() {
		for (int size : DIALOG_ICON_SIZES) {
			URL url = AppIcons.class.getResource(String.format(ICON_PATH_FORMAT, size));
			if (url != null) {
				return new ImageIcon(url);
			}
		}
		return null;
	}

	public static void install(JFrame frame) {
		List<Image> images = loadIconImages();
		if (images.isEmpty()) {
			return;
		}

		frame.setIconImages(images);

		if (Taskbar.isTaskbarSupported()) {
			Taskbar taskbar = Taskbar.getTaskbar();
			if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
				taskbar.setIconImage(images.getLast());
			}
		}
	}

	private static List<Image> loadIconImages() {
		List<Image> images = new ArrayList<>();
		Toolkit toolkit = Toolkit.getDefaultToolkit();

		for (int size : SIZES) {
			URL url = AppIcons.class.getResource(String.format(ICON_PATH_FORMAT, size));
			if (url != null) {
				images.add(toolkit.getImage(url));
			}
		}

		return images;
	}
}
