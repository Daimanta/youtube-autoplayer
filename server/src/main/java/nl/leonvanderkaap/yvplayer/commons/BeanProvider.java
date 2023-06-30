package nl.leonvanderkaap.yvplayer.commons;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
public class BeanProvider {

    @Bean
    public Executor executor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("PlayerThread-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate buildRestTemplate() {
        return new RestTemplate();
    }
}
