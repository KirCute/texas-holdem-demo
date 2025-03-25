package top.kircute.texas.middleware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.kircute.texas.pojo.Pair;
import top.kircute.texas.service.RoomBO;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class DelayedExecutor extends Thread {
    @Resource
    private ConcurrentHashMap<String, RoomBO> rooms;

    private final PriorityBlockingQueue<Pair<Long, RoomBO.LongReflectionAutoFoldCallback>> tasks;
    private final Lock lock;
    private boolean running;

    public DelayedExecutor() {
        super("DelayedExecutorProcessor");
        tasks = new PriorityBlockingQueue<>(100, Comparator.comparingLong(Pair::getFirst));
        lock = new ReentrantLock();
    }

    @Override
    public void run() {
        while (running) {
            try {
                long now = System.currentTimeMillis();
                Pair<Long, RoomBO.LongReflectionAutoFoldCallback> cb = tasks.take();
                lock.lock();
                if (Thread.interrupted() && !running) break;  // Clear duplicate interrupt
                if (!tasks.isEmpty() && tasks.peek().getFirst() < cb.getFirst()) {  // Double-Checked Locking Pattern
                    tasks.put(cb);
                    cb = tasks.take();
                }
                if (cb.getFirst() > now) {
                    tasks.put(cb);
                    lock.unlock();
                    Thread.sleep(cb.getFirst() - now + 10L);
                    continue;
                }
                lock.unlock();
                log.info("{} started execution.", cb.getSecond());
                long next = cb.getSecond().execute(rooms);
                if (next > 0L) {
                    log.info("{} yielded, the next execution time is {}.", cb.getSecond(), next);
                    cb.setFirst(next);
                    tasks.put(cb);
                } else {
                    log.info("{} quited.", cb.getSecond());
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void put(long next, RoomBO.LongReflectionAutoFoldCallback cb) {
        lock.lock();
        boolean interrupt = !tasks.isEmpty() && tasks.peek().getFirst() > next;
        tasks.put(new Pair<>(next, cb));
        if (interrupt) this.interrupt();
        lock.unlock();
    }

    @PostConstruct
    public void postConstruct() {
        running = true;
        this.start();
    }

    @PreDestroy
    public void preDestroy() {
        running = false;
        this.interrupt();
    }
}
