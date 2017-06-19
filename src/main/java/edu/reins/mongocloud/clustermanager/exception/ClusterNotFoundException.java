package edu.reins.mongocloud.clustermanager.exception;

import edu.reins.mongocloud.exception.ClientException;

public class ClusterNotFoundException extends ClientException {
    public ClusterNotFoundException(String msg) {
        super(msg);
    }
}
