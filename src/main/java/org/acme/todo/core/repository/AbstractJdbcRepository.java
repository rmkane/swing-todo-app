package org.acme.todo.core.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractJdbcRepository<T, ID> implements CrudRepository<T, ID> {

	protected final JdbcTemplate jdbcTemplate;

	protected void requireAffectedRows(int affectedRows, String errorMessage) {
		if (affectedRows == 0) {
			throw new IllegalArgumentException(errorMessage);
		}
	}
}
