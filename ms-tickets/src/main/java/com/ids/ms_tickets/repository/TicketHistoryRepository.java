package com.ids.ms_tickets.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.ids.ms_tickets.model.TicketHistory;

/** Solo expone alta y consulta: el historial es append-only (RN-11). */
public interface TicketHistoryRepository extends Repository<TicketHistory, Long> {

	<S extends TicketHistory> S save(S history);

	List<TicketHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}
