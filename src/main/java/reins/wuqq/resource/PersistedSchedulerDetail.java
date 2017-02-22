package reins.wuqq.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.mesos.state.ZooKeeperState;
import org.springframework.beans.factory.annotation.Autowired;
import reins.wuqq.model.Instance;
import reins.wuqq.support.PersistedState;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class PersistedSchedulerDetail extends PersistedState<SchedulerDetail> {
    private static final String SCHEDULER_TASKS_VAR = "schedulerTasks";

    @Autowired
    private static ObjectMapper mapper;

    private static final Supplier<SchedulerDetail> DEFAULT_VALUE_GENERATOR = () -> new SchedulerDetail();
    private static final Function<SchedulerDetail, byte[]> DEFAULT_SERIALIZER = input -> {
        try {
            return mapper.writeValueAsBytes(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
    private static final Function<byte[], SchedulerDetail> DEFAULT_DESERIALIZER = input -> {
        try {
            return mapper.readValue(input, SchedulerDetail.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    @Autowired
    public PersistedSchedulerDetail(@Nonnull final ZooKeeperState state) {
        super(SCHEDULER_TASKS_VAR, state, DEFAULT_VALUE_GENERATOR, DEFAULT_DESERIALIZER, DEFAULT_SERIALIZER);
    }

    public void addPendingTask(@Nonnull final Instance instance) {
        val tasks = get();

        tasks.getPendingTasks().add(instance);

        setValue(tasks);
    }

    public void setPendingTasks(@Nonnull final List<Instance> tasks) {
        setValue(get());
    }

    public List<Instance> getPendingTasks() {
        return get().getPendingTasks();
    }
}
