package org.acme.todo.events;

import org.acme.todo.settings.AppSettings;

public record SettingsChangedEvent(AppSettings settings) {
}
