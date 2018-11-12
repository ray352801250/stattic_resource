package com.dodoca.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	
	@Value("${dodoca.rest.read_timeout}")
	int read_timeout;
	
	@Value("${dodoca.rest.connection_timeout}")
	int connection_timeout;
	
	@Bean(name="php_restTemplate")
    public RestTemplate restTemplate(@Qualifier("php_httpRequestFactory")ClientHttpRequestFactory factory) {
		
		return new RestTemplate(factory);
        
    }

	@Bean(name="php_httpRequestFactory")
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(read_timeout);
        factory.setConnectTimeout(connection_timeout);
        return factory;
    }
}
