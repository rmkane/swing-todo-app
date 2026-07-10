package org.acme.todo.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.acme.todo.settings.AppSettings;
import org.acme.todo.settings.SettingsService;
import org.acme.todo.ui.theme.LookAndFeelSupport;

@Lazy
@Component
public class SettingsDialog extends JDialog {

	private final SettingsService settingsService;

	private final JComboBox<String> themeField = new JComboBox<>(new String[]{"system", "light", "dark"});

	private final JCheckBox confirmDeleteField = new JCheckBox("Confirm before deleting");

	private final JCheckBox showCompletedField = new JCheckBox("Show completed todos");

	private final JSpinner windowWidthField = new JSpinner(new SpinnerNumberModel(800, 400, 4000, 25));

	private final JSpinner windowHeightField = new JSpinner(new SpinnerNumberModel(600, 300, 3000, 25));

	public SettingsDialog(SettingsService settingsService) {
		super((Frame) null, "Settings", true);

		this.settingsService = settingsService;

		configureDialog();
	}

	public void open(Frame owner) {
		setLocationRelativeTo(owner);
		loadSettings();
		setVisible(true);
	}

	private void configureDialog() {
		setLayout(new BorderLayout());
		add(createFormPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.SOUTH);

		pack();
		setResizable(false);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}

	private JPanel createFormPanel() {
		JPanel panel = new JPanel(new GridBagLayout());

		addRow(panel, 0, "Theme:", themeField);
		addRow(panel, 1, "Window width:", windowWidthField);
		addRow(panel, 2, "Window height:", windowHeightField);

		GridBagConstraints constraints = createConstraints(3);
		constraints.gridwidth = 2;
		panel.add(confirmDeleteField, constraints);

		constraints = createConstraints(4);
		constraints.gridwidth = 2;
		panel.add(showCompletedField, constraints);

		return panel;
	}

	private JPanel createButtonPanel() {
		JButton cancelButton = new JButton("Cancel");
		JButton saveButton = new JButton("Save");

		cancelButton.addActionListener(event -> setVisible(false));
		saveButton.addActionListener(event -> saveSettings());

		JPanel panel = new JPanel();
		panel.add(cancelButton);
		panel.add(saveButton);

		return panel;
	}

	private void addRow(JPanel panel, int row, String label, java.awt.Component field) {
		GridBagConstraints labelConstraints = createConstraints(row);
		labelConstraints.gridx = 0;
		labelConstraints.anchor = GridBagConstraints.LINE_END;

		GridBagConstraints fieldConstraints = createConstraints(row);
		fieldConstraints.gridx = 1;
		fieldConstraints.anchor = GridBagConstraints.LINE_START;

		panel.add(new JLabel(label), labelConstraints);
		panel.add(field, fieldConstraints);
	}

	private GridBagConstraints createConstraints(int row) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row;
		constraints.insets = new Insets(6, 8, 6, 8);
		return constraints;
	}

	private void loadSettings() {
		AppSettings settings = settingsService.getSettings();

		themeField.setSelectedItem(settings.theme());
		confirmDeleteField.setSelected(settings.confirmDelete());
		showCompletedField.setSelected(settings.showCompleted());
		windowWidthField.setValue(settings.windowWidth());
		windowHeightField.setValue(settings.windowHeight());
	}

	private void saveSettings() {
		try {
			AppSettings settings = new AppSettings((String) themeField.getSelectedItem(),
					confirmDeleteField.isSelected(), showCompletedField.isSelected(),
					(Integer) windowWidthField.getValue(), (Integer) windowHeightField.getValue());

			settingsService.save(settings);
			LookAndFeelSupport.apply(settings.theme());
			LookAndFeelSupport.refreshAllWindows();
			setVisible(false);
		} catch (RuntimeException exception) {
			JOptionPane.showMessageDialog(this, exception.getMessage(), "Could not save settings",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
