package org.acme.todo.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.springframework.context.annotation.Lazy;

import org.acme.todo.settings.SettingsService;
import org.acme.todo.todo.Todo;
import org.acme.todo.todo.TodoService;

@Lazy
@org.springframework.stereotype.Component
public class TodoPanel extends JPanel {

	private final TodoService todoService;
	private final SettingsService settingsService;

	private final TodoListModel listModel = new TodoListModel();
	private final JList<Todo> todoList = new JList<>(listModel);
	private final JTextField descriptionField = new JTextField();

	public TodoPanel(TodoService todoService, SettingsService settingsService) {
		this.todoService = todoService;
		this.settingsService = settingsService;

		configureLayout();
		configureActions();
		refreshTodos();
	}

	private void configureLayout() {
		setLayout(new BorderLayout(8, 8));

		todoList.setCellRenderer(new TodoCellRenderer());

		add(new JScrollPane(todoList), BorderLayout.CENTER);
		add(createEntryPanel(), BorderLayout.NORTH);
		add(createButtonPanel(), BorderLayout.SOUTH);
	}

	private JPanel createEntryPanel() {
		JButton addButton = new JButton("Add");

		addButton.addActionListener(event -> addTodo());
		descriptionField.addActionListener(event -> addTodo());

		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.add(descriptionField, BorderLayout.CENTER);
		panel.add(addButton, BorderLayout.EAST);

		return panel;
	}

	private JPanel createButtonPanel() {
		JButton toggleButton = new JButton("Toggle completed");
		JButton deleteButton = new JButton("Delete");

		toggleButton.addActionListener(event -> toggleSelectedTodo());
		deleteButton.addActionListener(event -> deleteSelectedTodo());

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(toggleButton);
		panel.add(deleteButton);

		return panel;
	}

	private void configureActions() {
		todoList.addListSelectionListener(event -> {
			// Future selection-dependent behavior can go here.
		});
	}

	private void addTodo() {
		runUiAction(() -> {
			todoService.add(descriptionField.getText());
			descriptionField.setText("");
			refreshTodos();
		});
	}

	private void toggleSelectedTodo() {
		Todo todo = todoList.getSelectedValue();

		if (todo == null) {
			return;
		}

		runUiAction(() -> {
			todoService.setCompleted(todo.id(), !todo.completed());
			refreshTodos();
		});
	}

	private void deleteSelectedTodo() {
		Todo todo = todoList.getSelectedValue();

		if (todo == null || !confirmDelete(todo)) {
			return;
		}

		runUiAction(() -> {
			todoService.delete(todo.id());
			refreshTodos();
		});
	}

	private boolean confirmDelete(Todo todo) {
		if (!settingsService.getSettings().confirmDelete()) {
			return true;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete \"" + todo.description() + "\"?", "Delete Todo",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		return result == JOptionPane.YES_OPTION;
	}

	private void refreshTodos() {
		List<Todo> todos = todoService.findAll();

		if (!settingsService.getSettings().showCompleted()) {
			todos = todos.stream().filter(todo -> !todo.completed()).toList();
		}

		listModel.setTodos(todos);
	}

	public void reloadTodos() {
		refreshTodos();
	}

	private void runUiAction(Runnable action) {
		try {
			action.run();
		} catch (RuntimeException exception) {
			JOptionPane.showMessageDialog(this, exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static class TodoCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
				boolean focused) {
			super.getListCellRendererComponent(list, value, index, selected, focused);

			if (value instanceof Todo todo) {
				setText(todo.description());

				Font baseFont = getFont();
				int style = todo.completed() ? Font.ITALIC : Font.PLAIN;

				setFont(baseFont.deriveFont(style));
			}

			return this;
		}
	}
}
