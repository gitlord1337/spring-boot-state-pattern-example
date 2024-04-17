package org.code1337.statepatterntest.config.pojo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitMQQueueConfig {
    private List<RabbitMQQueue> queues;

    public List<RabbitMQQueue> getQueues() {
        return queues;
    }

    public void setQueues(List<RabbitMQQueue> queues) {
        this.queues = queues;
    }
}