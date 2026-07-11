package org.acme.todo.ui;

import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import org.acme.todo.events.CategoriesChangedEvent;
import org.acme.todo.events.SettingsChangedEvent;
import org.acme.todo.events.TodosChangedEvent;

@Lazy
@Component
@RequiredArgsConstructor
public class UiRefreshCoordinator {

	private final TodoPanel todoPanel;

	@EventListener
	public void onTodosChanged(TodosChangedEvent event) {
		runOnEdt(todoPanel::reloadTodos);
	}

	@EventListener
	public void onCategoriesChanged(CategoriesChangedEvent event) {
		runOnEdt(() -> {
			todoPanel.reloadCategories();
			todoPanel.reloadTodos();
		});
	}

	@EventListener
	public void onSettingsChanged(SettingsChangedEvent event) {
		runOnEdt(todoPanel::reloadTodos);
	}

	private void runOnEdt(Runnable action) {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}

		SwingUtilities.invokeLater(action);
	}
}
