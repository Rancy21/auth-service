package com.larr.auth.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class HttpConfig {

    @Bean
    public WebServerFactoryCustomizer<
        TomcatServletWebServerFactory
    > httpsRedirect() {
        return factory -> {
            Connector connector = new Connector(
                TomcatServletWebServerFactory.DEFAULT_PROTOCOL
            );
            connector.setScheme("http");
            connector.setPort(8080);
            connector.setSecure(false);
            connector.setRedirectPort(8443);
            factory.addAdditionalConnectors(connector);
        };
    }
}
