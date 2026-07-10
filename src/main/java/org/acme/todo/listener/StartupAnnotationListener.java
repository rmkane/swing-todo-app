package org.acme.todo.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StartupAnnotationListener {

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady(ApplicationReadyEvent event) {
		log.info("Application is ready: appName={}, activeProfiles={}",
				event.getApplicationContext().getApplicationName(),
				String.join(",", event.getApplicationContext().getEnvironment().getActiveProfiles()));
	}
}
