package com.ids.ms_catalog.repository;

import com.ids.ms_catalog.model.Priority;
import com.ids.ms_catalog.model.PriorityCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PriorityRepository extends JpaRepository<Priority, Long> {

	boolean existsByCode(PriorityCode code);

	boolean existsByLevel(Short level);

	@Query("select p from Priority p left join fetch p.sla order by p.level")
	List<Priority> findAllWithSlaOrderByLevel();

	@Query("select p from Priority p left join fetch p.sla where p.id = :id")
	Optional<Priority> findByIdWithSla(@Param("id") Long id);
}
