# Spring Boot ActiveMQ Broker

This demo shows the bare minimum needed to configure ActiveMQ to run as a Spring Boot app, it doesnt take much. Most of the
work is handled by the `XBeanBrokerFactory` provided by the `activemq-spring` dependency. This lets us define the broker 
configuration in the familiar [`activemq.xml`](./src/main/resources/activemq.xml) form. The rest is mostly boiler plate.

Spring Boot Autoconfiguration is not necessary, you could instantiate the bean in any Spring way. Component scanning for
java configuration, spring bean xml files...The point is that you can bring ActiveMQ into the normal Spring realm where
you can start making use of all the typical Spring features you know and love.


## Building and Running

```bash
> mvn spring-boot:run
```

or

```bash
> mvn install
> java -jar target/activemq-spring-boot-1.0.0.jar
```