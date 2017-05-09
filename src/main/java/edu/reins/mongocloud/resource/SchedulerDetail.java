package edu.reins.mongocloud.resource;

import edu.reins.mongocloud.model.Instance;
import lombok.Data;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Data
public class SchedulerDetail {
    @Nonnull
    List<Instance> pendingTasks = new ArrayList<>();
}
