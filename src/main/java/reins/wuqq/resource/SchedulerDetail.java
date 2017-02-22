package reins.wuqq.resource;

import lombok.Data;
import reins.wuqq.model.Instance;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Data
public class SchedulerDetail {
    @Nonnull
    List<Instance> pendingTasks = new ArrayList<>();
}
