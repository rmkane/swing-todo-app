package org.acme.todo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.acme.todo.core.model.TodoCategory;
import org.acme.todo.core.service.TodoService;

@Lazy
@Component
public class CategoryManagerDialog extends JDialog {

	private final TodoService todoService;

	private final DefaultListModel<TodoCategory> listModel = new DefaultListModel<>();
	private final JList<TodoCategory> categoryList = new JList<>(listModel);
	private final JTextField nameField = new JTextField(16);
	private final JTextField colorField = new JTextField("#3B82F6", 8);
	private final JButton updateButton = new JButton("Update Selected");

	public CategoryManagerDialog(TodoService todoService) {
		super((Frame) null, "Manage Categories", true);
		this.todoService = todoService;
		configureDialog();
	}

	public boolean open(Frame owner) {
		setLocationRelativeTo(owner);
		reloadCategories();
		setVisible(true);
		return true;
	}

	private void configureDialog() {
		setLayout(new BorderLayout(8, 8));
		add(createCategoryListPanel(), BorderLayout.CENTER);
		add(createEditorPanel(), BorderLayout.SOUTH);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setSize(460, 360);
	}

	private JPanel createCategoryListPanel() {
		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
		categoryList.addListSelectionListener(event -> syncEditorFromSelection());

		JButton deleteButton = new JButton("Delete Selected");
		deleteButton.addActionListener(event -> deleteSelectedCategory());

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actions.add(deleteButton);
		panel.add(actions, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel createEditorPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(6, 8, 6, 8);
		constraints.gridy = 0;

		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.LINE_END;
		panel.add(new JLabel("Name:"), constraints);

		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.LINE_START;
		panel.add(nameField, constraints);

		constraints.gridx = 2;
		panel.add(new JLabel("Color:"), constraints);

		constraints.gridx = 3;
		panel.add(colorField, constraints);

		JButton pickColorButton = new JButton("Pick...");
		pickColorButton.addActionListener(event -> chooseColor());
		constraints.gridx = 4;
		panel.add(pickColorButton, constraints);

		JButton addButton = new JButton("Add Category");
		addButton.addActionListener(event -> addCategory());
		constraints.gridx = 5;
		panel.add(addButton, constraints);

		updateButton.setEnabled(false);
		updateButton.addActionListener(event -> updateSelectedCategory());
		constraints.gridx = 6;
		panel.add(updateButton, constraints);

		return panel;
	}

	private void chooseColor() {
		Color chosen = JColorChooser.showDialog(this, "Choose Category Color", parseColor(colorField.getText()));
		if (chosen == null) {
			return;
		}

		colorField.setText(String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue()));
	}

	private void addCategory() {
		try {
			todoService.addCategory(nameField.getText(), colorField.getText());
			resetEditor();
			reloadCategories();
		} catch (RuntimeException exception) {
			JOptionPane.showMessageDialog(this, exception.getMessage(), "Could not add category",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void updateSelectedCategory() {
		TodoCategory selected = categoryList.getSelectedValue();
		if (selected == null) {
			return;
		}

		try {
			todoService.updateCategory(selected.id(), nameField.getText(), colorField.getText());
			reloadCategories();
			selectCategoryById(selected.id());
		} catch (RuntimeException exception) {
			JOptionPane.showMessageDialog(this, exception.getMessage(), "Could not update category",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteSelectedCategory() {
		TodoCategory selected = categoryList.getSelectedValue();
		if (selected == null) {
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete category \"" + selected.name() + "\"?",
				"Delete Category", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.YES_OPTION) {
			return;
		}

		todoService.deleteCategory(selected.id());
		reloadCategories();
		resetEditor();
	}

	private void reloadCategories() {
		listModel.clear();
		List<TodoCategory> categories = todoService.findCategories();
		for (TodoCategory category : categories) {
			listModel.addElement(category);
		}
		updateButton.setEnabled(categoryList.getSelectedValue() != null);
	}

	private void syncEditorFromSelection() {
		TodoCategory selected = categoryList.getSelectedValue();
		if (selected == null) {
			updateButton.setEnabled(false);
			return;
		}

		nameField.setText(selected.name());
		colorField.setText(selected.color());
		updateButton.setEnabled(true);
	}

	private void selectCategoryById(long categoryId) {
		for (int index = 0; index < listModel.getSize(); index++) {
			TodoCategory category = listModel.get(index);
			if (category.id() != null && category.id() == categoryId) {
				categoryList.setSelectedIndex(index);
				return;
			}
		}

		categoryList.clearSelection();
	}

	private void resetEditor() {
		nameField.setText("");
		colorField.setText("#3B82F6");
		categoryList.clearSelection();
		updateButton.setEnabled(false);
	}

	private Color parseColor(String value) {
		try {
			return Color.decode(value);
		} catch (RuntimeException ignored) {
			return new Color(0x3B82F6);
		}
	}
}
