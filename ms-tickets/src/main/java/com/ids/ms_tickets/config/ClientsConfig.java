package com.ids.ms_tickets.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import com.ids.ms_tickets.client.BearerTokenRelayInterceptor;
import com.ids.ms_tickets.client.CatalogClient;
import com.ids.ms_tickets.client.ProviderErrors;
import com.ids.ms_tickets.client.UsersClient;
import com.ids.ms_tickets.config.properties.ClientsProperties;

@Configuration
public class ClientsConfig {

	@Bean
	UsersClient usersClient(ClientsProperties properties, BearerTokenRelayInterceptor relay,
			ProviderErrors errors) {
		return httpClient(UsersClient.class, properties.users().url(), properties, relay, errors);
	}

	@Bean
	CatalogClient catalogClient(ClientsProperties properties, BearerTokenRelayInterceptor relay,
			ProviderErrors errors) {
		return httpClient(CatalogClient.class, properties.catalog().url(), properties, relay, errors);
	}

	private static <T> T httpClient(Class<T> type, String baseUrl, ClientsProperties properties,
			BearerTokenRelayInterceptor relay, ProviderErrors errors) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		RestClient restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.requestInterceptor(relay)
				.defaultStatusHandler(ProviderErrors::appliesTo, errors)
				.build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.build()
				.createClient(type);
	}
}
