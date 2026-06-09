package backend.academy.linktracker.configuration;

import backend.academy.linktracker.dto.LinkUpdate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfiguration {

    @Bean(name = "linkUpdateDlqKafkaTemplate")
    public KafkaTemplate<Long, Object> linkUpdateDlqKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> producerProperties = Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        Map<Class<?>, Serializer<?>> serializers = new LinkedHashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(LinkUpdate.class, new JacksonJsonSerializer<LinkUpdate>());
        serializers.put(Object.class, new JacksonJsonSerializer<>());

        DefaultKafkaProducerFactory<Long, Object> producerFactory = new DefaultKafkaProducerFactory<>(
                producerProperties, new LongSerializer(), new DelegatingByTypeSerializer(serializers, true));

        return new KafkaTemplate<>(producerFactory);
    }
}
