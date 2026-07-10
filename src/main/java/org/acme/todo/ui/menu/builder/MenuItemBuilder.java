package org.acme.todo.ui.menu.builder;

import javax.swing.JMenuItem;

public final class MenuItemBuilder {

	private final JMenuItem item;

	MenuItemBuilder(String text) {
		item = new JMenuItem(text);
	}

	public MenuItemBuilder mnemonic(char mnemonic) {
		item.setMnemonic(mnemonic);
		return this;
	}

	public MenuItemBuilder displayedMnemonicIndex(int index) {
		item.setDisplayedMnemonicIndex(index);
		return this;
	}

	public MenuItemBuilder onClick(Runnable action) {
		item.addActionListener(event -> action.run());
		return this;
	}

	JMenuItem item() {
		return item;
	}
}
