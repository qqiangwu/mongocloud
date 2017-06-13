package edu.reins.mongocloud.resource.daemon;

import edu.reins.mongocloud.Daemon;
import edu.reins.mongocloud.support.annotation.Nothrow;
import lombok.extern.slf4j.Slf4j;
import org.apache.mesos.MesosSchedulerDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;

@Daemon
@Slf4j
@ThreadSafe
public class Reconciler {
    @Autowired
    private MesosSchedulerDriver schedulerDriver;

    // Implicit reconciling
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = 60 * 1000 * 5)
    @Nothrow
    public void sync() {
        LOG.info("reconcile");

        schedulerDriver.reconcileTasks(Collections.emptySet());
    }
}