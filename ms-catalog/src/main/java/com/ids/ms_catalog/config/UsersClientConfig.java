package com.ids.ms_catalog.config;

import com.ids.ms_catalog.client.UsersClient;
import com.ids.ms_catalog.config.properties.ClientsProperties;
import com.ids.ms_catalog.security.BearerTokenRelayInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(ClientsProperties.class)
public class UsersClientConfig {

	@Bean
	public UsersClient usersClient(ClientsProperties properties) {
		ClientsProperties.ServiceClient users = properties.users();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(users.connectTimeout());
		requestFactory.setReadTimeout(users.readTimeout());
		RestClient restClient = RestClient.builder()
				.baseUrl(users.url())
				.requestFactory(requestFactory)
				.requestInterceptor(new BearerTokenRelayInterceptor())
				.build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
				.createClient(UsersClient.class);
	}
}
