package com.github.macgregor.activemq;

import org.apache.activemq.broker.BrokerService;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

public class BrokerAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BrokerAutoConfig.class));

    @Test
    public void brokerIsEmbeddedByDefault(){
        this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(BrokerService.class);
                    BrokerService broker = context.getBean(BrokerService.class);
                    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(broker.isStarted()).isTrue());
                });
    }

    @Test
    public void configurationBacksOffWhenBrokerServiceExists() {
        this.contextRunner
                .withUserConfiguration(OverrideBrokerServiceConfiguration.class)
                .run((context) -> assertThat(
                        mockingDetails(context.getBean(BrokerService.class)).isMock())
                        .isTrue());
    }

    @Configuration
    static class EmptyConfiguration {

    }

    @Configuration
    static class OverrideBrokerServiceConfiguration {

        @Bean
        public BrokerService broker() {
            return mock(BrokerService.class);
        }

    }
}
