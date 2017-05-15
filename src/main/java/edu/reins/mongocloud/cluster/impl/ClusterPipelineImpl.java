package edu.reins.mongocloud.cluster.impl;

import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.cluster.ClusterPipeline;
import edu.reins.mongocloud.cluster.ClusterPipelineProcessor;
import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.command.Command;
import edu.reins.mongocloud.exception.ClientException;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Daemon
@ThreadSafe
@Slf4j
public class ClusterPipelineImpl implements ClusterPipeline, Runnable {
    BlockingQueue<Command> pipeline = new LinkedBlockingQueue<>();

    @Autowired
    ApplicationContext context;

    @Autowired
    Map<ClusterState, ClusterPipelineProcessor> processors;

    ClusterPipelineProcessor currentProcessor;

    @PostConstruct
    public void setup() {
        log.info("initialized");

        currentProcessor = processors.get(ClusterState.STARTING);
    }

    @Override
    public void post(@Nonnull final Command command) {
        log.debug("recvCommand");

        pipeline.offer(command);
    }

    // Running in its own thread! Not thread safe
    @Override
    public void run() {
        log.info("pipelineStarted");

        prepare();

        ClusterState state = ClusterState.STARTING;

        while (state != ClusterState.CLOSING) {
            try {
                val rs = processOne();

                if (rs.isPresent() && !rs.get().equals(state)) {
                    log.info("stateChanged(state: {})", rs.get());

                    currentProcessor.deactive();

                    state = rs.get();
                    currentProcessor = processors.get(state);

                    currentProcessor.active();
                }
            } catch (InterruptedException e) {
                log.info("interrupted");
                state = ClusterState.CLOSING;
            }
        }

        log.info("pipelineStopped(state: {})", state);

        clearPendingCommands();
        exit(state);
    }

    private void prepare() {
        Thread.currentThread().setName("ClusterPipeline");

        currentProcessor.active();
    }

    private Optional<ClusterState> processOne() throws InterruptedException {
        @Cleanup("finished")
        val cmd = pipeline.take();

        try {
            log.info("pipeline(cmd: {})", cmd.getType());

            return Optional.of(currentProcessor.process(cmd));
        } catch (ClientException e) {
            cmd.setException(e);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.error("unknownException", e);
        }

        return Optional.of(ClusterState.CLOSING);
    }

    private void clearPendingCommands() {
        pipeline.forEach(Command::finished);
        pipeline = null;
    }

    private void exit(final ClusterState state) {
        SpringApplication.exit(context, () -> state == ClusterState.CLOSING? 0: 1);
    }
}
