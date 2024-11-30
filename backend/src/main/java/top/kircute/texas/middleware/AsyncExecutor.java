package top.kircute.texas.middleware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import top.kircute.texas.utils.ThrowableRunnable;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class AsyncExecutor {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void execute(ThrowableRunnable func) {
        executor.execute(() -> {
            try {
                func.run();
            } catch (Exception e) {
                log.error("Failed execute async task: {}", e.getMessage());
            }
        });
    }

    public void pushMessage(WebSocketSession session, String msg) {
        this.execute(() -> {
            synchronized (session) {
                session.sendMessage(new TextMessage(msg));
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        executor.shutdown();
    }
}
