package org.acme.todo.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;

import org.acme.todo.core.model.Todo;
import org.acme.todo.core.model.TodoCategory;
import org.acme.todo.core.service.TodoService;
import org.acme.todo.events.TodosChangedEvent;
import org.acme.todo.settings.SettingsService;

@Lazy
@org.springframework.stereotype.Component
public class TodoPanel extends JPanel {

	@Serial
	private static final long serialVersionUID = 5255332170132990774L;

	private final TodoService todoService;
	private final SettingsService settingsService;
	private final CategoryManagerDialog categoryManagerDialog;
	private final ApplicationEventPublisher eventPublisher;

	private final TodoListModel listModel = new TodoListModel();
	private final JList<Todo> todoList = new JList<>(listModel);
	private final JTextField descriptionField = new JTextField();
	private final JComboBox<TodoCategory> categoryField = new JComboBox<>();

	public TodoPanel(TodoService todoService, SettingsService settingsService,
			CategoryManagerDialog categoryManagerDialog, ApplicationEventPublisher eventPublisher) {
		this.todoService = todoService;
		this.settingsService = settingsService;
		this.categoryManagerDialog = categoryManagerDialog;
		this.eventPublisher = eventPublisher;

		configureLayout();
		configureActions();
		reloadCategories();
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
		JButton manageCategoriesButton = new JButton("Categories...");

		addButton.addActionListener(event -> addTodo());
		manageCategoriesButton.addActionListener(event -> openCategoryManager());
		descriptionField.addActionListener(event -> addTodo());
		categoryField.setPrototypeDisplayValue(new TodoCategory(0L, "Long Category Name", "#000000"));

		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.add(descriptionField, BorderLayout.CENTER);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		right.add(categoryField);
		right.add(manageCategoriesButton);
		right.add(addButton);
		panel.add(right, BorderLayout.EAST);

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
			Todo created = todoService.add(descriptionField.getText());

			TodoCategory category = (TodoCategory) categoryField.getSelectedItem();
			if (category != null) {
				todoService.setCategories(created.id(), List.of(category.id()));
			}

			descriptionField.setText("");
			eventPublisher.publishEvent(new TodosChangedEvent("todo-add"));
		});
	}

	private void toggleSelectedTodo() {
		Todo todo = todoList.getSelectedValue();

		if (todo == null) {
			return;
		}

		runUiAction(() -> {
			todoService.setCompleted(todo.id(), !todo.completed());
			eventPublisher.publishEvent(new TodosChangedEvent("todo-toggle-completed"));
		});
	}

	private void deleteSelectedTodo() {
		Todo todo = todoList.getSelectedValue();

		if (todo == null || !confirmDelete(todo)) {
			return;
		}

		runUiAction(() -> {
			todoService.delete(todo.id());
			eventPublisher.publishEvent(new TodosChangedEvent("todo-delete"));
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

	public void reloadCategories() {
		TodoCategory selected = (TodoCategory) categoryField.getSelectedItem();
		List<TodoCategory> categories = todoService.findCategories();

		categoryField.removeAllItems();
		categoryField.addItem(null);
		for (TodoCategory category : categories) {
			categoryField.addItem(category);
		}

		if (selected != null) {
			for (int i = 1; i < categoryField.getItemCount(); i++) {
				TodoCategory candidate = categoryField.getItemAt(i);
				if (candidate != null && candidate.id().equals(selected.id())) {
					categoryField.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	private void openCategoryManager() {
		categoryManagerDialog.open((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this));
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
				setText(buildLabel(todo));

				Font baseFont = getFont();
				int style = todo.completed() ? Font.ITALIC : Font.PLAIN;

				setFont(baseFont.deriveFont(style));
			}

			return this;
		}

		private String buildLabel(Todo todo) {
			StringBuilder builder = new StringBuilder("<html>");
			builder.append(escapeHtml(todo.description()));

			List<TodoCategory> categories = todo.categories() == null ? List.of() : todo.categories();
			if (!categories.isEmpty()) {
				builder.append(" <span style='color:#666666'>&middot;</span> ");
				List<String> tags = new ArrayList<>();
				for (TodoCategory category : categories) {
					String color = normalizeHexColor(category.color());
					tags.add("<span style='color:" + color + ";font-weight:bold'>" + escapeHtml(category.name())
							+ "</span>");
				}
				builder.append(String.join(" ", tags));
			}

			builder.append("</html>");
			return builder.toString();
		}

		private String normalizeHexColor(String color) {
			if (color == null) {
				return "#666666";
			}

			try {
				Color decoded = Color.decode(color);
				return String.format("#%02X%02X%02X", decoded.getRed(), decoded.getGreen(), decoded.getBlue());
			} catch (RuntimeException ignored) {
				return "#666666";
			}
		}

		private String escapeHtml(String value) {
			return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}
}
