package top.kircute.texas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kircute.texas.service.RoomBO;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class DataSourceConfig {
    @Bean
    public ConcurrentHashMap<String, RoomBO> rooms() {
        return new ConcurrentHashMap<>();
    }
}
