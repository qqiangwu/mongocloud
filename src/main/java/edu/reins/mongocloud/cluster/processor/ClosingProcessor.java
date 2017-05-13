package edu.reins.mongocloud.cluster.processor;

import edu.reins.mongocloud.cluster.ClusterState;
import org.springframework.stereotype.Component;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A dummy processor which actually will not be executed.
 */
@Component
@NotThreadSafe
public class ClosingProcessor extends AbstractPipelineProcessor {

    public ClosingProcessor() {
        super(ClusterState.CLOSING);
    }
}
