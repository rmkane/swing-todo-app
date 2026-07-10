package org.acme.todo.ui.menu.builder;

import java.util.function.UnaryOperator;

import javax.swing.JMenuBar;

public final class MenuBarBuilder {

	private final JMenuBar menuBar = new JMenuBar();

	private MenuBarBuilder() {
	}

	public static MenuBarBuilder create() {
		return new MenuBarBuilder();
	}

	public MenuBarBuilder menu(String name, UnaryOperator<MenuBuilder> configure) {
		MenuBuilder menuBuilder = configure.apply(new MenuBuilder(name));
		menuBar.add(menuBuilder.menu());
		return this;
	}

	public JMenuBar build() {
		return menuBar;
	}
}
