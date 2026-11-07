package com.ids.ms_tickets.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(nullable = false, length = 4000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketStatus status;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "priority_id", nullable = false)
	private Long priorityId;

	@Column(name = "priority_code", nullable = false, length = 20)
	private String priorityCode;

	@Column(name = "sla_response_hours", nullable = false)
	private Integer slaResponseHours;

	@Column(name = "sla_resolution_hours", nullable = false)
	private Integer slaResolutionHours;

	@Column(name = "response_due_at", nullable = false)
	private Instant responseDueAt;

	@Column(name = "resolution_due_at", nullable = false)
	private Instant resolutionDueAt;

	@Column(name = "requester_id", nullable = false)
	private Long requesterId;

	@Column(name = "assignee_id")
	private Long assigneeId;

	@Column(name = "first_response_at")
	private Instant firstResponseAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Column(name = "closed_at")
	private Instant closedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
