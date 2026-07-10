package org.acme.todo.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class SettingsConfiguration {

	@Bean
	public Path configDirectory() {
		return AppDirectories.resolveConfigDirectory();
	}
}
