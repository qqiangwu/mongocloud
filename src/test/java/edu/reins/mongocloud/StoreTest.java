package edu.reins.mongocloud;

import edu.reins.mongocloud.impl.StoreImpl;
import lombok.Value;
import lombok.val;
import org.apache.mesos.state.InMemoryState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;

public class StoreTest {
    private Store store;

    @Before
    public void setup() {
        store = new StoreImpl();

        ReflectionTestUtils.setField(store, "persistentState", new InMemoryState());
    }

    @Value
    public static final class Entity implements Serializable {
        private int x;
        private int y;
    }

    @Test
    public void testInAndOut() {
        val e = new Entity(0, 100);

        store.put("x", e);

        val r = store.get("x", Entity.class);

        Assert.assertTrue(r.isPresent());
        Assert.assertEquals(e, r.get());
    }
}
