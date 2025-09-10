package org.rutebanken.irkalla.config;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.http.HttpComponent;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static org.rutebanken.irkalla.Constants.ET_CLIENT_ID_HEADER;
import static org.rutebanken.irkalla.Constants.ET_CLIENT_NAME_HEADER;

@Configuration
public class HttpClientConfig {

    @Value("${http.client.name:irkalla}")
    private String clientName;

    @Value("${HOSTNAME:irkalla}")
    private String clientId;

    @Bean
    public HttpClientConfigurer httpClientConfigurer(@Autowired CamelContext camelContext) {
        HttpComponent httpComponent = camelContext.getComponent("http", HttpComponent.class);
        HttpClientConfigurer httpClientConfigurer = httpClientBuilder -> httpClientBuilder.setDefaultHeaders(
                Arrays.asList(new BasicHeader(ET_CLIENT_ID_HEADER, clientId), new BasicHeader(ET_CLIENT_NAME_HEADER, clientName)));

        httpComponent.setHttpClientConfigurer(httpClientConfigurer);
        return httpClientConfigurer;
    }

}