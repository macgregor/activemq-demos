package com.github.macgregor.activemq.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.region.MessageReference;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageId;
import org.apache.activemq.jaas.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Broker plugin that logs information and statistics about a message, useful for tracking messages as they enter
 * and exit a system.
 */
public class MessageBreadcrumbLoggingBroker extends BrokerFilter {

    private static Logger logger = LoggerFactory.getLogger(MessageBreadcrumbLoggingBroker.class);
    private static ObjectMapper mapper;

    public MessageBreadcrumbLoggingBroker(Broker next) {
        super(next);
    }

    @Override
    public void send(ProducerBrokerExchange producerExchange, Message messageSend) throws Exception {
        Map<String, Object> extraProperties = new LinkedHashMap<>();
        extraProperties.put("percentageBlocked", producerExchange.getTotalTimeBlocked());
        try {
            //https://issues.apache.org/jira/browse/AMQ-7017
            extraProperties.put("totalTimeBlocked", producerExchange.getPercentageBlocked());
        } catch(ArithmeticException e){}

        log(producerExchange.getConnectionContext(), messageSend.getMessageHardRef(), "producer-send", extraProperties);

        super.send(producerExchange, messageSend);
    }

    @Override
    public void messageConsumed(ConnectionContext context, MessageReference messageReference) {
        log(context, messageReference.getMessageHardRef(), "message-consumed");
        super.messageConsumed(context, messageReference);
    }

    @Override
    public void messageDelivered(ConnectionContext context,MessageReference messageReference) {
        log(context, messageReference.getMessageHardRef(), "message-delivered");
        super.messageDelivered(context, messageReference);
    }

    @Override
    public void messageDiscarded(ConnectionContext context, Subscription sub, MessageReference messageReference) {
        log(context, messageReference.getMessageHardRef(), "message-discarded");
        super.messageDiscarded(context, sub, messageReference);
    }

    @Override
    public void messageExpired(ConnectionContext context, MessageReference message, Subscription subscription) {
        log(context, message.getMessageHardRef(), "message-expired");
        super.messageExpired(context, message, subscription);
    }

    @Override
    public boolean sendToDeadLetterQueue(ConnectionContext context, MessageReference messageReference, Subscription subscription, Throwable poisonCause) {
        Map<String, Object> extraProperties = new LinkedHashMap<>();
        extraProperties.put("poisonCause", poisonCause.getMessage());

        log(context, messageReference.getMessage(), "send-to-dlq", extraProperties);

        return super.sendToDeadLetterQueue(context, messageReference, subscription, poisonCause);
    }

    protected void log(ConnectionContext context, Message message, String op){
        log(context, message, op, Collections.emptyMap());
    }
    protected void log(ConnectionContext context, Message message, String op, Map<String, Object> extraProperties) {
        if(AdvisorySupport.isAdvisoryTopic(message.getDestination())){
            logger.debug(createLogString(context, message, op, extraProperties, false));
        } else {
            logger.info(createLogString(context, message, op, extraProperties, false));
        }
    }

     protected String createLogString(ConnectionContext context, Message message, String op, Map<String, Object> extraProperties, boolean logBody){
        Map<String, Object> logMap = new LinkedHashMap<>();

        // if the client did not set one, add a jms correlation id to help track message across systems/destination hops
        String correlationId = message.getCorrelationId();
        if(correlationId == null){
            correlationId = UUID.randomUUID().toString();
            message.setCorrelationId(correlationId);
        }
        logMap.put("correlationId", correlationId);
        logMap.put("op", op);

        String wireFormatProviderName = Optional.ofNullable(context)
                .map(c -> c.getWireFormatInfo())
                .map(w -> {
                    try {return w.getProviderName();}
                    catch (IOException e){ return null; }
                }).orElse(null);
        String wireFormatProviderVersion = Optional.ofNullable(context)
                .map(c -> c.getWireFormatInfo())
                .map(w -> {
                    try {return w.getProviderVersion();}
                    catch (IOException e){ return null; }
                }).orElse(null);
        logMap.put("providerName", wireFormatProviderName);
        logMap.put("providerVersion", wireFormatProviderVersion);


        String username = Optional.ofNullable(context)
                .map(c -> c.getSecurityContext())
                .map(c -> c.getUserName())
                .orElse(null);
        logMap.put("username", username);

        Set<Principal> roles = Optional.ofNullable(context)
                        .map(c -> c.getSecurityContext())
                        .map(c -> c.getPrincipals())
                        .orElse(Collections.emptySet());
        List<String> roleNames = roles.stream()
                .filter(p -> p instanceof GroupPrincipal)
                .map(p -> p.getName())
                .collect(Collectors.toList());
         logMap.put("roles", roleNames);

        String clientIp = Optional.ofNullable(context)
                .map(c -> c.getConnectionState())
                .map(c -> c.getInfo())
                .map(i -> i.getClientIp())
                .orElse(null);
        logMap.put("clientIp", clientIp);
        String clientId = Optional.ofNullable(context)
                .map(c -> c.getConnectionState())
                .map(c -> c.getInfo())
                .map(i -> i.getClientId())
                .orElse(null);
        logMap.put("clientId", clientId);

        MessageId messageId = message.getMessageId();
        try {
            Map<String, String> stringProperties = new HashMap<>(message.getProperties().size());
            for(String property : message.getProperties().keySet()){
                try {
                    stringProperties.put(property, ((ActiveMQMessage)message).getStringProperty(property));
                } catch (JMSException e) {
                    logger.warn("Unable to convert property to string: property name - {}, activemq message type - {}, property type - {}",
                            property, message.getClass().getSimpleName(), message.getProperties().get(property).getClass().getSimpleName());
                }
            }
            logMap.put("messageProperties", stringProperties);
        } catch (IOException e) {}
        logMap.put("messageType", message.getType());
        logMap.put("messageExpiration", message.getExpiration());
        logMap.put("brokerPath", message.getBrokerPath());
        logMap.put("brokerInTime", message.getBrokerInTime());
        logMap.put("brokerOutTime", message.getBrokerOutTime());
        logMap.put("messageSize", message.getSize());

        logMap.putAll(extraProperties);


        if(logBody && message instanceof ActiveMQTextMessage){
            try {
                logMap.put("body", ((ActiveMQTextMessage)message).getText());
            } catch (JMSException e) {
                logger.warn("Unable to get message body for " + correlationId, e);
            }
        }

        ObjectMapper mapper = jsonMapper();
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(logMap);
            return json;
        } catch (JsonProcessingException e) {
            logger.warn(String.format("Unable to convert log message %s to json, falling back to letting the logger handle things.", correlationId), e);
            return logMap.toString();
        }
    }

    public static ObjectMapper jsonMapper(){
        if(mapper == null) {
            mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        }
        return mapper;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        MessageBreadcrumbLoggingBroker.logger = logger;
    }
}
