package com.ids.ms_catalog.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "priorities")
@Getter
@Setter
@NoArgsConstructor
public class Priority {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "code", nullable = false, length = 20, unique = true)
	private PriorityCode code;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "level", nullable = false, unique = true)
	private Short level;

	@Column(name = "active", nullable = false)
	private boolean active;

	@OneToOne(mappedBy = "priority", cascade = CascadeType.ALL, orphanRemoval = true)
	private SlaPolicy sla;
}
