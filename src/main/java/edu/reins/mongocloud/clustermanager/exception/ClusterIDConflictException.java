package edu.reins.mongocloud.clustermanager.exception;

import edu.reins.mongocloud.exception.ClientException;
import edu.reins.mongocloud.model.ClusterID;

public class ClusterIDConflictException extends ClientException {
    public ClusterIDConflictException(final ClusterID msg) {
        super(msg.getValue());
    }
}
