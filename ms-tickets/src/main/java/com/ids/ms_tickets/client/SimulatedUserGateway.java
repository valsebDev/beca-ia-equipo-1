package com.ids.ms_tickets.client;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.ids.ms_tickets.exception.BusinessRuleException;

/** Simulación de ms-users hasta la Práctica 3: solo los IDs listados son AGENTE activos. */
@Component
public class SimulatedUserGateway implements UserGateway {

	private static final Set<Long> ACTIVE_AGENTS = Set.of(5L, 6L, 7L);

	@Override
	public void ensureActiveAgent(Long userId) {
		if (!ACTIVE_AGENTS.contains(userId)) {
			throw new BusinessRuleException("ASSIGNEE_INVALID",
					"El usuario " + userId + " no es un agente activo");
		}
	}
}
