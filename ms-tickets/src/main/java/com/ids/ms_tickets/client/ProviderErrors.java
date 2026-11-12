package com.ids.ms_tickets.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ids.ms_tickets.exception.DependencyUnavailableException;

import lombok.extern.slf4j.Slf4j;

/** Traduce los fallos de ms-users y ms-catalog a 503; el 404 lo interpreta cada gateway. */
@Slf4j
@Component
public class ProviderErrors implements RestClient.ResponseSpec.ErrorHandler {

	public static boolean appliesTo(HttpStatusCode status) {
		return status.isError() && status.value() != HttpStatus.NOT_FOUND.value();
	}

	@Override
	public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
		HttpStatusCode status = response.getStatusCode();
		if (status.value() == HttpStatus.UNAUTHORIZED.value()
				|| status.value() == HttpStatus.FORBIDDEN.value()) {
			log.warn("Un servicio dependiente rechazó la llamada con estado {}", status.value());
		}
		throw new DependencyUnavailableException("No se pudo validar la información con un servicio dependiente");
	}
}
