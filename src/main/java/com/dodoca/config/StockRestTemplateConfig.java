package com.dodoca.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class StockRestTemplateConfig {
	
	@Value("${dodoca_rest_stock_read_timeout}")
	int readTimeout;
	
	@Value("${dodoca_rest_stock_connection_timeout}")
	int connectionTimeout;
	
	@Bean(name="stock_restTemplate")
    public RestTemplate restTemplate(@Qualifier("stock_httpRequestFactory")ClientHttpRequestFactory factory) {
		
		return new RestTemplate(factory);
        
    }

	@Bean(name="stock_httpRequestFactory")
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(connectionTimeout);
        return factory;
    }
}
