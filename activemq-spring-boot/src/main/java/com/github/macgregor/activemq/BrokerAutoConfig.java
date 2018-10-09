package com.github.macgregor.activemq;


import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.xbean.XBeanBrokerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@AutoConfigureBefore(ActiveMQAutoConfiguration.class)
@ConditionalOnClass({ BrokerService.class })
@ConditionalOnMissingBean(BrokerService.class)
public class BrokerAutoConfig {

    private static final Logger logger = LoggerFactory.getLogger(BrokerAutoConfig.class);

    @Value("${activemq.config:classpath:activemq.xml}")
    private String activemqConfig;

    @Value("${spring.config.location:classpath:application.yml}")
    private String springConfig;

    @Bean(initMethod="start", destroyMethod = "stop")
    public BrokerService umbBroker() throws Exception {
        System.setProperty("spring.config.location", springConfig); //make sure system property is set so activemq.xml can resolve the path
        XBeanBrokerFactory brokerFactory = new XBeanBrokerFactory();
        return brokerFactory.createBroker(new URI(activemqConfig));
    }
}