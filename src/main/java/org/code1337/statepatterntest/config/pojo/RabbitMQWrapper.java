package org.code1337.statepatterntest.config.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RabbitMQWrapper {
    private String queueName;
    private String exchangeName;
    private String routingKeyName;
    private Queue queue;
    private TopicExchange topicExchange;
    private Binding binding;
}
