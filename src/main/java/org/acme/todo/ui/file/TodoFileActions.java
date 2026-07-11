package org.acme.todo.ui.file;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.todo.core.model.Todo;
import org.acme.todo.core.model.TodoCategory;
import org.acme.todo.core.service.TodoService;
import org.acme.todo.events.CategoriesChangedEvent;
import org.acme.todo.events.TodosChangedEvent;

@Slf4j
@RequiredArgsConstructor
@org.springframework.stereotype.Component
public class TodoFileActions {

	private final TodoService todoService;
	private final ApplicationEventPublisher eventPublisher;
	private final ObjectMapper objectMapper;

	public void exportTodos(Component parent) {
		JFileChooser chooser = createChooser("Export Todos", "todos.json");
		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		Path file = chooser.getSelectedFile().toPath();
		List<Todo> todos = todoService.findAll();
		List<TodoCategory> categories = todoService.findCategories();

		try {
			if (file.getParent() != null) {
				Files.createDirectories(file.getParent());
			}

			ExportDocument document = new ExportDocument(1, Instant.now().toString(), categories.stream()
					.map(category -> new ExportCategory(category.id(), category.name(), category.color())).toList(),
					todos.stream()
							.map(todo -> new ExportTodo(todo.id(), todo.description(), todo.completed(),
									todo.createdAt() == null ? null : todo.createdAt().toString(),
									todo.completedAt() == null ? null : todo.completedAt().toString(),
									todo.categories().stream().map(TodoCategory::id).toList()))
							.toList());

			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), document);
			log.info("Exported {} todos to {}", todos.size(), file.toAbsolutePath());
			JOptionPane.showMessageDialog(parent, "Exported " + todos.size() + " todos.", "Export complete",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (RuntimeException | IOException exception) {
			log.warn("Failed to export todos to {}", file.toAbsolutePath(), exception);
			JOptionPane.showMessageDialog(parent, "Could not export todos: " + exception.getMessage(), "Export failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void importTodos(Component parent) {
		JFileChooser chooser = createChooser("Import Todos", null);
		if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		Path file = chooser.getSelectedFile().toPath();
		ImportDocument imported;

		try {
			imported = objectMapper.readValue(file.toFile(), ImportDocument.class);
		} catch (IOException exception) {
			log.warn("Failed to import todos from {}", file.toAbsolutePath(), exception);
			JOptionPane.showMessageDialog(parent, "Could not import todos: " + exception.getMessage(), "Import failed",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		List<ImportTodo> todos = imported.todos() == null ? List.of() : imported.todos();
		List<ImportCategory> categories = imported.categories() == null ? List.of() : imported.categories();

		if (todos.isEmpty()) {
			JOptionPane.showMessageDialog(parent, "No todos found in selected file.", "Import",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		Object[] options = {"Append", "Replace", "Cancel"};
		int choice = JOptionPane.showOptionDialog(parent,
				"How should imported todos be applied?\nAppend keeps existing todos. Replace clears existing todos first.",
				"Import options", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
			return;
		}

		boolean replace = choice == 1;

		try {
			if (replace) {
				todoService.deleteAll();
				todoService.deleteAllCategories();
			}

			Map<Long, Long> categoryIdMap = new HashMap<>();
			for (ImportCategory category : categories) {
				if (category == null) {
					continue;
				}

				TodoCategory resolved = todoService.findOrCreateCategory(category.name(), category.color());
				if (category.id() != null) {
					categoryIdMap.put(category.id(), resolved.id());
				}
			}

			for (ImportTodo todo : todos) {
				List<Long> categoryIds = (todo.categoryIds() == null ? List.<Long>of() : todo.categoryIds()).stream()
						.map(id -> categoryIdMap.getOrDefault(id, id)).distinct().collect(Collectors.toList());

				todoService.addWithMetadata(todo.description(), todo.completed(), parseInstant(todo.createdAt()),
						parseInstant(todo.completedAt()), categoryIds);
			}

			eventPublisher.publishEvent(new CategoriesChangedEvent("todo-import"));
			eventPublisher.publishEvent(new TodosChangedEvent("todo-import"));
			log.info("Imported {} todos from {} (mode={})", todos.size(), file.toAbsolutePath(),
					replace ? "replace" : "append");
			JOptionPane.showMessageDialog(parent, "Imported " + todos.size() + " todos.", "Import complete",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (RuntimeException exception) {
			log.warn("Import failed for {}", file.toAbsolutePath(), exception);
			JOptionPane.showMessageDialog(parent, "Could not import todos: " + exception.getMessage(), "Import failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private JFileChooser createChooser(String title, String suggestedName) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
		if (suggestedName != null) {
			chooser.setSelectedFile(new java.io.File(suggestedName));
		}
		return chooser;
	}

	private Instant parseInstant(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return Instant.parse(value);
	}

	private record ExportDocument(int version, String exportedAt, List<ExportCategory> categories,
			List<ExportTodo> todos) {
	}

	private record ExportCategory(Long id, String name, String color) {
	}

	private record ExportTodo(Long id, String description, boolean completed, String createdAt, String completedAt,
			List<Long> categoryIds) {
	}

	private record ImportDocument(int version, String exportedAt, List<ImportCategory> categories,
			List<ImportTodo> todos) {
	}

	private record ImportCategory(Long id, String name, String color) {
	}

	private record ImportTodo(Long id, String description, boolean completed, String createdAt, String completedAt,
			List<Long> categoryIds) {
	}
}
