package backend.academy.linktracker.configuration;

import backend.academy.linktracker.dto.LinkUpdate;
import backend.academy.linktracker.exception.InvalidLinkUpdateMessageException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerConfiguration {

    private final KafkaTopicProperties topicProperties;
    private final KafkaConsumerProperties consumerProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, LinkUpdate> linkUpdateKafkaListenerContainerFactory(
            ConsumerFactory<Long, LinkUpdate> consumerFactory,
            @Qualifier("linkUpdateDlqKafkaTemplate") KafkaOperations<Long, Object> kafkaOperations) {
        ConcurrentKafkaListenerContainerFactory<Long, LinkUpdate> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(linkUpdateErrorHandler(kafkaOperations));

        return factory;
    }

    private DefaultErrorHandler linkUpdateErrorHandler(KafkaOperations<Long, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations, (record, exception) -> new TopicPartition(topicProperties.getLinkUpdatesDlq(), -1));

        FixedBackOff backOff = new FixedBackOff(
                consumerProperties.getRetryInterval().toMillis(), consumerProperties.getRetryAttempts());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                InvalidLinkUpdateMessageException.class, DeserializationException.class, SerializationException.class);

        return errorHandler;
    }
}
