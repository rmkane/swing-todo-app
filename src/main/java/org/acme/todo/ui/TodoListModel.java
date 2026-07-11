package org.acme.todo.ui;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import org.acme.todo.core.model.Todo;

public class TodoListModel extends AbstractListModel<Todo> {

	@Serial
	private static final long serialVersionUID = 7097178925642507258L;

	private final List<Todo> todos = new ArrayList<>();

	@Override
	public int getSize() {
		return todos.size();
	}

	@Override
	public Todo getElementAt(int index) {
		return todos.get(index);
	}

	public void setTodos(List<Todo> newTodos) {
		int previousSize = todos.size();

		todos.clear();
		todos.addAll(newTodos);

		int maximumSize = Math.max(previousSize, todos.size());

		if (maximumSize > 0) {
			fireContentsChanged(this, 0, maximumSize - 1);
		}
	}

	public Todo get(int index) {
		return todos.get(index);
	}
}
