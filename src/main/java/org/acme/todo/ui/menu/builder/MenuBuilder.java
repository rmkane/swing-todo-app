package org.acme.todo.ui.menu.builder;

import java.util.function.UnaryOperator;

import javax.swing.JMenu;

public final class MenuBuilder {

	private final JMenu menu;

	MenuBuilder(String name) {
		menu = new JMenu(name);
	}

	JMenu menu() {
		return menu;
	}

	public MenuBuilder mnemonic(char mnemonic) {
		menu.setMnemonic(mnemonic);
		return this;
	}

	public MenuBuilder displayedMnemonicIndex(int index) {
		menu.setDisplayedMnemonicIndex(index);
		return this;
	}

	public MenuBuilder item(String text, UnaryOperator<MenuItemBuilder> configure) {
		MenuItemBuilder itemBuilder = configure.apply(new MenuItemBuilder(text));
		menu.add(itemBuilder.item());
		return this;
	}

	public MenuBuilder separator() {
		menu.addSeparator();
		return this;
	}
}
