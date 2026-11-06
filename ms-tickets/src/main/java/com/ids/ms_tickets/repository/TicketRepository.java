package com.ids.ms_tickets.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ids.ms_tickets.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	List<Ticket> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

	List<Ticket> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId);

	List<Ticket> findAllByOrderByCreatedAtDesc();
}
