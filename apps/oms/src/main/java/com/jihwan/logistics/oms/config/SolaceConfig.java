package com.jihwan.logistics.oms.config;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Slf4j
@Configuration
public class SolaceConfig {

    @Value("${SOLACE_HOST}")
    private String host;

    @Value("${SOLACE_VPN}")
    private String vpn;

    @Value("${SOLACE_USER}")
    private String username;

    @Value("${SOLACE_PASS}")
    private String password;

    @Bean
    public ConnectionFactory solaceConnectionFactory() throws Exception {
        SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory();

        connectionFactory.setHost(host);
        connectionFactory.setVPN(vpn);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        log.info("📡 Solace JmsTemplate configured for host: {}", host);


        return connectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory solaceConnectionFactory) {
        return new JmsTemplate(solaceConnectionFactory);
    }

}
