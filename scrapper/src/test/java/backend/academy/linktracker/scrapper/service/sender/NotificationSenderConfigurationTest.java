package backend.academy.linktracker.scrapper.service.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.properties.KafkaTopicProperties;
import backend.academy.linktracker.scrapper.service.sender.impl.FallbackNotificationSender;
import backend.academy.linktracker.scrapper.service.sender.impl.HttpNotificationSender;
import backend.academy.linktracker.scrapper.service.sender.impl.KafkaNotificationSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

class NotificationSenderConfigurationTest {

    @SuppressWarnings("unchecked")
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationSenderTestConfiguration.class)
            .withBean(BotClient.class, () -> mock(BotClient.class))
            .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class));

    @Test
    void shouldUseFallbackNotificationSender_whenTransportIsHttp() {
        contextRunner.withPropertyValues("app.notification.transport=HTTP").run(context -> {
            assertThat(context).hasSingleBean(HttpNotificationSender.class);
            assertThat(context).hasSingleBean(KafkaNotificationSender.class);
            assertThat(context).hasSingleBean(FallbackNotificationSender.class);
            assertThat(context.getBean(NotificationSender.class)).isInstanceOf(FallbackNotificationSender.class);
        });
    }

    @Test
    void shouldUseKafkaNotificationSender_whenTransportIsKafka() {
        contextRunner.withPropertyValues("app.notification.transport=KAFKA").run(context -> {
            assertThat(context).doesNotHaveBean(HttpNotificationSender.class);
            assertThat(context).doesNotHaveBean(FallbackNotificationSender.class);
            assertThat(context).hasSingleBean(NotificationSender.class);
            assertThat(context.getBean(NotificationSender.class)).isInstanceOf(KafkaNotificationSender.class);
        });
    }

    @Test
    void shouldUseKafkaNotificationSender_whenTransportIsNotConfigured() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(HttpNotificationSender.class);
            assertThat(context).doesNotHaveBean(FallbackNotificationSender.class);
            assertThat(context).hasSingleBean(NotificationSender.class);
            assertThat(context.getBean(NotificationSender.class)).isInstanceOf(KafkaNotificationSender.class);
        });
    }

    @Configuration
    @Import({HttpNotificationSender.class, KafkaNotificationSender.class, FallbackNotificationSender.class})
    @EnableConfigurationProperties(KafkaTopicProperties.class)
    static class NotificationSenderTestConfiguration {}
}
