# Spring Boot ActiveMQ Broker

Demo app of running an activemq broker using Spring Boot as the container.


## Building and Running

```bash
> mvn spring-boot:run
```

or

```bash
> mvn install
> java -jar target/activemq-spring-boot-1.0.0.jar
```

## Spring Goodies

```bash
> curl http://localhost:8080/actuator/health
{"status":"UP"}
> curl http://localhost:8080/actuator/info
{"version":"1.0.0","build":{"version":"1.0.0","artifact":"activemq-spring-boot","name":"activemq-spring-boot","group":"com.github.macgregor","time":"2018-10-09T22:03:33.614Z"}}
```

You get `/actuator/health` and `/actuator/info` out of the box just by adding some dependencies to your pom. The health 
check is even establishing a jms connection to ensure ActiveMQ is running properly. You can enable many more actuator 
endpoints with just some configuration in `application.yml`. 

More info on Spring Boot Actuator:
* [Spring Boot Actuator: Production-ready Endpoints](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html)
* [Spring Boot Actuator: Production-ready JMX](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-jmx.html)
* [Spring Boot Actuator - Baeldung](https://www.baeldung.com/spring-boot-actuators)