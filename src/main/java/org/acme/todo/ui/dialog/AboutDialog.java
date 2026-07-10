package org.acme.todo.ui.dialog;

import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.acme.todo.App;
import org.acme.todo.ui.support.AppIcons;

@Lazy
@Component
public class AboutDialog {

	private static final String APP_NAME = "ACME Todo";
	private static final String APP_DESCRIPTION = "A desktop app for managing your todo list.";

	public void open(Window parent) {
		String body = "<html><b>" + APP_NAME + "</b><br>Version: " + version() + "<br><br>" + APP_DESCRIPTION
				+ "</html>";

		JLabel message = new JLabel(body);
		message.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		Icon icon = AppIcons.dialogIcon();
		JOptionPane.showMessageDialog(parent, message, "About " + APP_NAME, JOptionPane.INFORMATION_MESSAGE, icon);
	}

	private String version() {
		String version = App.class.getPackage().getImplementationVersion();
		return version != null ? version : "development";
	}
}
