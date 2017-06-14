package edu.reins.mongocloud.cluster;

import edu.reins.mongocloud.model.ClusterID;
import edu.reins.mongocloud.model.InstanceDefinition;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.core.env.Environment;

@UtilityClass
public final class Clusters {
    public static final String ENV_RS = "RS";
    public static final String ENV_CONFIG = "CONFIG";

    public static ClusterID of(final String id) {
        return new ClusterID(id);
    }

    public InstanceDefinition loadDefinition(final Environment env, final String prefix) {
        try {
            val inst = InstanceDefinition.class.newInstance();

            for (val field: InstanceDefinition.class.getDeclaredFields()) {
                val name = field.getName();
                val value = env.getProperty(String.format("%s.%s", prefix, name), field.getType());

                field.setAccessible(true);
                field.set(inst, value);
            }

            return inst;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}