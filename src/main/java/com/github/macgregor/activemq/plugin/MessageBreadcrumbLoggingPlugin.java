package com.github.macgregor.activemq.plugin;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBreadcrumbLoggingPlugin implements BrokerPlugin {

    private static Logger logger = LoggerFactory.getLogger(MessageBreadcrumbLoggingPlugin.class);

    @Override
    public Broker installPlugin(Broker next) throws Exception {
        logger.info("MessageBreadcrumbLoggingPlugin Loaded");
        return new MessageBreadcrumbLoggingBroker(next);
    }
}
