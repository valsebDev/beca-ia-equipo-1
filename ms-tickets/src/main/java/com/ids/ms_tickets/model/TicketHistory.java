package com.ids.ms_tickets.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ticket_history")
public class TicketHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 20)
	private HistoryEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private TicketStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", length = 20)
	private TicketStatus toStatus;

	@Column(name = "from_assignee_id")
	private Long fromAssigneeId;

	@Column(name = "to_assignee_id")
	private Long toAssigneeId;

	@Column(name = "changed_by", nullable = false)
	private Long changedBy;

	@Column(length = 1000)
	private String comment;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;
}
