package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PollingExecutorConfiguration {

    @Bean(name = "linkPollingExecutor", destroyMethod = "shutdown")
    public ExecutorService linkPollingExecutor(SchedulerProperties schedulerProperties) {
        return Executors.newFixedThreadPool(schedulerProperties.getParallelism());
    }
}
