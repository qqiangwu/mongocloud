package edu.reins.mongocloud.cluster.exception;

import edu.reins.mongocloud.cluster.ClusterState;
import edu.reins.mongocloud.cluster.command.CommandType;
import edu.reins.mongocloud.exception.internal.ProgrammingException;

import javax.annotation.Nonnull;

public class IllegalTransitionException extends ProgrammingException {
    public IllegalTransitionException(@Nonnull final ClusterState state, @Nonnull final CommandType command) {
        super(String.format("state=%s, command=%s", state, command));
    }
}
