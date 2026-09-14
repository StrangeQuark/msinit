package com.example.emailservice.kafka;

import com.example.emailservice.email.EmailRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Collection;
import java.util.List;

@Configuration
public class KafkaConfiguration {
    @Bean
    public Collection<NewTopic> kafkaTopics() {
        return List.of(
                TopicBuilder.name("general-email-events").partitions(1).replicas(1).build(),
                TopicBuilder.name("template-email-events").partitions(1).replicas(1).build()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailRequest> kafkaListenerContainerFactory(ConsumerFactory<String, EmailRequest> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EmailRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));

        return factory;
    }
}
