package edu.reins.mongocloud.impl;

import edu.reins.mongocloud.Actor;
import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.Event;
import edu.reins.mongocloud.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author wuqq
 */
@Daemon
@Slf4j
public class AsyncEventBus implements EventBus {
    private final Map<Class, Actor> eventDispatchers = new ConcurrentHashMap<>();
    private final BlockingQueue<Event> eventQueue = new ArrayBlockingQueue<>(1024);

    @Autowired
    private ExecutorService executorService;

    @PostConstruct
    public void setup() {
        executorService.submit(new PipelineProcessor());
    }

    @Override
    public <E extends Event> void register(final Class<E> eventType, final Actor<E> actor) {
        eventDispatchers.put(eventType, actor);
    }

    @Override
    public void post(final Event event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            log.info("eventBus post interrupted");
        }
    }

    private class PipelineProcessor implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final Event event = eventQueue.take();

                    handle(event);
                } catch (InterruptedException e) {
                    log.info("pipelineProcessor interrupted");
                    return;
                } catch (RuntimeException e) {
                    log.warn("unexpected exception(msg: {})", e.getMessage(), e);
                }
            }
        }

        private void handle(final Event event) {
            final Class<? extends Event> eventType = event.getClass();
            final Actor receiver = eventDispatchers.get(eventType);

            if (receiver == null) {
                log.warn("no receiver for event(event: {})", eventType.getSimpleName());
                return;
            }

            receiver.handle(event);
        }
    }
}
