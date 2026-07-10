package org.acme.todo.core.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

	Optional<T> findById(ID id);

	List<T> findAll();

	void deleteById(ID id);

	void deleteAll();
}
