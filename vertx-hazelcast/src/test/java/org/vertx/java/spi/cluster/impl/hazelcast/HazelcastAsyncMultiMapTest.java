package org.vertx.java.spi.cluster.impl.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.*;
import com.hazelcast.instance.GroupProperties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.spi.Action;
import org.vertx.java.core.spi.cluster.ChoosableIterable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.instance.HazelcastTestUtil.closeConnectionBetween;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:andreas.berger@kiwigrid.com">Andreas Berger</a>
 */
public class HazelcastAsyncMultiMapTest {

  private HazelcastInstance h1;
  private HazelcastInstance h2;

  @Before
  public void setup() {
    Config config = new Config();
    config.setProperty(GroupProperties.PROP_MERGE_FIRST_RUN_DELAY_SECONDS, "5");
    config.setProperty(GroupProperties.PROP_MERGE_NEXT_RUN_DELAY_SECONDS, "3");
    config.getGroupConfig().setName(UUID.randomUUID().toString());

    NetworkConfig networkConfig = config.getNetworkConfig();
    JoinConfig join = networkConfig.getJoin();
    join.getMulticastConfig().setEnabled(false);
    join.getTcpIpConfig().setEnabled(true);
    join.getTcpIpConfig().addMember("127.0.0.1");

    h1 = Hazelcast.newHazelcastInstance(config);
    h2 = Hazelcast.newHazelcastInstance(config);
  }

  @After
  public void cleanUp() {
    h1.shutdown();
    h2.shutdown();
  }


  @Test
  public void testSplitBrain() throws InterruptedException {
    final CountDownLatch splitLatch = new CountDownLatch(1);
    h2.getCluster().addMembershipListener(new SplitListener(splitLatch));

    final CountDownLatch mergeLatch = new CountDownLatch(1);
    h2.getLifecycleService().addLifecycleListener(new MergedEventLifeCycleListener(mergeLatch));

    DefaultVertx vertx = new DefaultVertx() {
      @Override
      public <T> void executeBlocking(Action<T> action, Handler<AsyncResult<T>> asyncResultHandler) {
        T result = action.perform();
        if (asyncResultHandler != null)
          asyncResultHandler.handle(new DefaultFutureResult<>(result));
      }
    };
    IMap<String, Set<String>> hzMap1 = h1.getMap("foo");
    HazelcastAsyncMultiMap<String, String> map1 = new HazelcastAsyncMultiMap<>(vertx, h1, "foo");
    IMap<String, Set<String>> hzMap2 = h2.getMap("foo");
    HazelcastAsyncMultiMap<String, String> map2 = new HazelcastAsyncMultiMap<>(vertx, h2, "foo");


    addValue(map1, "key", "v0");

    closeConnectionBetween(h1, h2);

    assertTrue(splitLatch.await(10, TimeUnit.SECONDS));
    Assert.assertEquals(1, h1.getCluster().getMembers().size());
    Assert.assertEquals(1, h2.getCluster().getMembers().size());

    addValue(map1, "key", "v1");
    addValue(map1, "key", "v2");

    addValue(map2, "key", "v1");
    addValue(map2, "key", "v3");

    removeValue(map2, "v0");

    assertTrue(mergeLatch.await(30, TimeUnit.SECONDS));
    Assert.assertEquals(2, h1.getCluster().getMembers().size());
    Assert.assertEquals(2, h2.getCluster().getMembers().size());

    Set<String> values = hzMap1.get("key");
    Assert.assertEquals(values, hzMap2.get("key"));
    assertEquals(values, map1);
    assertEquals(values, map2);

  }

  private void removeValue(HazelcastAsyncMultiMap<String, String> map, String value) throws InterruptedException {
    final CountDownLatch waitLatch = new CountDownLatch(1);
    map.removeAllForValue(value, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        waitLatch.countDown();
      }
    });
    waitLatch.await(5, TimeUnit.SECONDS);
  }

  private void addValue(HazelcastAsyncMultiMap<String, String> map, String key, String value) throws InterruptedException {
    final CountDownLatch waitLatch = new CountDownLatch(1);
    map.add(key, value, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        waitLatch.countDown();
      }
    });
    waitLatch.await(5, TimeUnit.SECONDS);
  }

  private void assertEquals(final Set<String> strings, HazelcastAsyncMultiMap<String, String> map) {
    map.get("key", new Handler<AsyncResult<ChoosableIterable<String>>>() {
      @Override
      public void handle(AsyncResult<ChoosableIterable<String>> event) {
        Set<String> l = new HashSet<>();
        for (String next : event.result()) {
          l.add(next);
        }
        Assert.assertEquals(strings, l);
      }
    });
  }

  private static class MergedEventLifeCycleListener implements LifecycleListener {

    private final CountDownLatch mergeLatch;

    public MergedEventLifeCycleListener(CountDownLatch mergeLatch) {
      this.mergeLatch = mergeLatch;
    }

    public void stateChanged(LifecycleEvent event) {
      if (event.getState() == LifecycleEvent.LifecycleState.MERGED) {
        mergeLatch.countDown();
      }
    }

  }

  private static class SplitListener implements MembershipListener {
    private final CountDownLatch splitLatch;

    public SplitListener(CountDownLatch splitLatch) {
      this.splitLatch = splitLatch;
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      splitLatch.countDown();
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
    }
  }

}