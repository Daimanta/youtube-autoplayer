package nl.leonvanderkaap.yvplayer.commons;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class BeanProvider {

    @Bean
    public Executor executor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public RestTemplate buildRestTemplate() {
        return new RestTemplate();
    }
}
