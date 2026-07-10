package org.acme.todo.ui.file;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import lombok.extern.slf4j.Slf4j;

import org.acme.todo.todo.Todo;
import org.acme.todo.todo.TodoService;
import org.acme.todo.ui.TodoPanel;

@Slf4j
@org.springframework.stereotype.Component
public class TodoFileActions {

	private final TodoService todoService;

	public TodoFileActions(TodoService todoService) {
		this.todoService = todoService;
	}

	public void exportTodos(Component parent) {
		JFileChooser chooser = createChooser("Export Todos", "todos.csv");
		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		Path file = chooser.getSelectedFile().toPath();
		List<Todo> todos = todoService.findAll();

		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			writer.write("description,completed");
			writer.newLine();

			for (Todo todo : todos) {
				writer.write(escape(todo.description()));
				writer.write(',');
				writer.write(Boolean.toString(todo.completed()));
				writer.newLine();
			}

			log.info("Exported {} todos to {}", todos.size(), file.toAbsolutePath());
			JOptionPane.showMessageDialog(parent, "Exported " + todos.size() + " todos.", "Export complete",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException exception) {
			log.warn("Failed to export todos to {}", file.toAbsolutePath(), exception);
			JOptionPane.showMessageDialog(parent, "Could not export todos: " + exception.getMessage(), "Export failed",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void importTodos(Component parent, TodoPanel todoPanel) {
		JFileChooser chooser = createChooser("Import Todos", null);
		if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		Path file = chooser.getSelectedFile().toPath();
		List<ImportedTodo> imported;

		try {
			imported = parse(file);
		} catch (IOException exception) {
			log.warn("Failed to import todos from {}", file.toAbsolutePath(), exception);
			JOptionPane.showMessageDialog(parent, "Could not import todos: " + exception.getMessage(), "Import failed",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (imported.isEmpty()) {
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
			}

			for (ImportedTodo row : imported) {
				Todo created = todoService.add(row.description());
				if (row.completed()) {
					todoService.setCompleted(created.id(), true);
				}
			}

			todoPanel.reloadTodos();
			log.info("Imported {} todos from {} (mode={})", imported.size(), file.toAbsolutePath(),
					replace ? "replace" : "append");
			JOptionPane.showMessageDialog(parent, "Imported " + imported.size() + " todos.", "Import complete",
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
		chooser.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
		if (suggestedName != null) {
			chooser.setSelectedFile(new java.io.File(suggestedName));
		}
		return chooser;
	}

	private List<ImportedTodo> parse(Path file) throws IOException {
		List<ImportedTodo> rows = new ArrayList<>();

		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String line;
			boolean first = true;

			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}

				if (first && line.toLowerCase().startsWith("description,")) {
					first = false;
					continue;
				}
				first = false;

				String[] parts = splitCsvLine(line);
				if (parts.length < 1) {
					continue;
				}

				String description = unescape(parts[0]).trim();
				if (description.isEmpty()) {
					continue;
				}

				boolean completed = parts.length > 1 && Boolean.parseBoolean(parts[1].trim());
				rows.add(new ImportedTodo(description, completed));
			}
		}

		return rows;
	}

	private String[] splitCsvLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}

			if (c == ',' && !inQuotes) {
				values.add(current.toString());
				current.setLength(0);
				continue;
			}

			current.append(c);
		}

		values.add(current.toString());
		return values.toArray(String[]::new);
	}

	private String escape(String value) {
		String escaped = value.replace("\"", "\"\"");
		if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
			return '"' + escaped + '"';
		}
		return escaped;
	}

	private String unescape(String value) {
		String trimmed = value.trim();
		if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
			trimmed = trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
		}
		return trimmed;
	}

	private record ImportedTodo(String description, boolean completed) {
	}
}
