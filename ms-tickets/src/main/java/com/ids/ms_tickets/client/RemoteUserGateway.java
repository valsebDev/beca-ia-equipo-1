package com.ids.ms_tickets.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ids.ms_tickets.client.dto.UserDto;
import com.ids.ms_tickets.exception.BusinessRuleException;
import com.ids.ms_tickets.exception.DependencyUnavailableException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RemoteUserGateway implements UserGateway {

	private static final String AGENT_ROLE = "AGENTE";

	private final UsersClient usersClient;

	@Override
	public void ensureActiveAgent(Long userId) {
		UserDto user;
		try {
			user = usersClient.getUser(userId);
		} catch (HttpClientErrorException.NotFound ex) {
			throw invalidAssignee(userId, ex);
		} catch (ResourceAccessException ex) {
			throw new DependencyUnavailableException("ms-users no responde", ex);
		}
		if (!user.active() || !AGENT_ROLE.equals(user.role())) {
			throw invalidAssignee(userId);
		}
	}

	private static BusinessRuleException invalidAssignee(Long userId) {
		return invalidAssignee(userId, null);
	}

	private static BusinessRuleException invalidAssignee(Long userId, Throwable cause) {
		return new BusinessRuleException("ASSIGNEE_INVALID",
				"El usuario " + userId + " no es un agente activo", cause);
	}
}
