package edu.reins.mongocloud.cluster.exception;

import edu.reins.mongocloud.exception.ClientException;

import javax.annotation.Nonnull;

public class ClusterUninitializedException extends ClientException {
    public ClusterUninitializedException(@Nonnull final String msg) {
        super(msg);
    }
}
