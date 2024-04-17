package org.code1337.statepatterntest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.code1337.statepatterntest.config.pojo.QueueType;
import org.code1337.statepatterntest.config.pojo.RabbitMQQueue;
import org.code1337.statepatterntest.config.pojo.RabbitMQQueueConfig;
import org.code1337.statepatterntest.config.pojo.RabbitMQWrapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    //see application.yml
    @Bean
    public Map<String, RabbitMQWrapper> postProcessBeanDefinitionRegistry(@Autowired RabbitMQQueueConfig queueConfig, @Autowired GenericWebApplicationContext context) throws BeansException {
        Map<String, RabbitMQWrapper> queueContainerMap = new HashMap<>();

        for (RabbitMQQueue queue : queueConfig.getQueues()) {
            String queueName = queue.getName();
            if (queue.isRequest()) {
                queueContainerMap.put(queueName + "Request", registerQueue(context, queueName, QueueType.REQUEST));
            }
            if (queue.isResponse()) {
                queueContainerMap.put(queueName + "Response", registerQueue(context, queueName, QueueType.RESPONSE));
            }
        }

        return queueContainerMap;
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter producerJackson2MessageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter);
        return rabbitTemplate;
    }

    public RabbitMQWrapper registerQueue(GenericWebApplicationContext context, String queue, QueueType queueType) {
        RabbitMQWrapper rabbitMQWrapper = new RabbitMQWrapper();
        String queueName = "q" + queue;
        String exchangeName = "e" + queue;
        String routingKey = "r" + queue;

        if (QueueType.REQUEST.equals(queueType)) {
            String queueNameRequest = queueName + "Request";
            String exchangeNameRequest = exchangeName + "Request";
            String routingKeyRequest = routingKey + "Request";

            context.registerBean(queueNameRequest, Queue.class, () -> new Queue(queueNameRequest, false));
            context.registerBean(exchangeNameRequest, TopicExchange.class, () -> new TopicExchange(exchangeNameRequest));
            context.registerBean(routingKeyRequest, Binding.class, () -> BindingBuilder.bind((Queue) context.getBean(queueNameRequest))
                    .to((TopicExchange) context.getBean(exchangeNameRequest))
                    .with(routingKeyRequest));

            rabbitMQWrapper.setQueue((Queue) context.getBean(queueNameRequest));
            rabbitMQWrapper.setTopicExchange((TopicExchange) context.getBean(exchangeNameRequest));
            rabbitMQWrapper.setBinding((Binding) context.getBean(routingKeyRequest));
            rabbitMQWrapper.setQueueName(queueNameRequest);
            rabbitMQWrapper.setExchangeName(exchangeNameRequest);
            rabbitMQWrapper.setRoutingKeyName(routingKeyRequest);
        } else if (QueueType.RESPONSE.equals(queueType)) {
            String queueNameResponse = queueName + "Response";
            String exchangeNameResponse = exchangeName + "Response";
            String routingKeyResponse = routingKey + "Response";

            context.registerBean(queueNameResponse, Queue.class, () -> new Queue(queueNameResponse, false));
            context.registerBean(exchangeNameResponse, TopicExchange.class, () -> new TopicExchange(exchangeNameResponse));
            context.registerBean(routingKeyResponse, Binding.class, () -> BindingBuilder.bind((Queue) context.getBean(queueNameResponse))
                    .to((TopicExchange) context.getBean(exchangeNameResponse))
                    .with(routingKeyResponse));

            rabbitMQWrapper.setQueue((Queue) context.getBean(queueNameResponse));
            rabbitMQWrapper.setTopicExchange((TopicExchange) context.getBean(exchangeNameResponse));
            rabbitMQWrapper.setBinding((Binding) context.getBean(routingKeyResponse));
            rabbitMQWrapper.setQueueName(queueNameResponse);
            rabbitMQWrapper.setExchangeName(exchangeNameResponse);
            rabbitMQWrapper.setRoutingKeyName(routingKeyResponse);
        }
        return rabbitMQWrapper;
    }
}
