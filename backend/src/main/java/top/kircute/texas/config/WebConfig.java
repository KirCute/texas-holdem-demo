package top.kircute.texas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import top.kircute.texas.controller.GameHandler;
import top.kircute.texas.interceptor.CorsInterceptor;
import top.kircute.texas.interceptor.ErrorForwardInterceptor;
import top.kircute.texas.interceptor.WebSocketAuthenticator;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebConfig implements WebMvcConfigurer, WebSocketConfigurer {
    @Resource
    private GameHandler gameHandler;

    @Resource
    private CorsInterceptor corsInterceptor;

    @Resource
    private ErrorForwardInterceptor errorForwardInterceptor;

    @Resource
    private WebSocketAuthenticator webSocketAuthenticator;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setUseLastModified(true);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameHandler,"/game_ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthenticator);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(corsInterceptor);
        registry.addInterceptor(errorForwardInterceptor);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
