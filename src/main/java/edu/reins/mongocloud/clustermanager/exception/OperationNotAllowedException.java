package edu.reins.mongocloud.clustermanager.exception;

import edu.reins.mongocloud.exception.ClientException;

public class OperationNotAllowedException extends ClientException {
    public OperationNotAllowedException(String msg) {
        super(msg);
    }
}
