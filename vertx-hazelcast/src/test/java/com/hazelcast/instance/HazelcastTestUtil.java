package com.hazelcast.instance;

import com.hazelcast.core.HazelcastInstance;

/**
 * @author <a href="mailto:andreas.berger@kiwigrid.com">Andreas Berger</a>
 */
public class HazelcastTestUtil {
  public static Node getNode(HazelcastInstance hz) {
    HazelcastInstanceImpl impl = getHazelcastInstanceImpl(hz);
    return impl != null ? impl.node : null;
  }

  public static HazelcastInstanceImpl getHazelcastInstanceImpl(HazelcastInstance hz) {
    HazelcastInstanceImpl impl = null;
    if (hz instanceof HazelcastInstanceProxy) {
      impl = ((HazelcastInstanceProxy) hz).original;
    } else if (hz instanceof HazelcastInstanceImpl) {
      impl = (HazelcastInstanceImpl) hz;
    }
    return impl;
  }

  public static void closeConnectionBetween(HazelcastInstance h1, HazelcastInstance h2) {
    if (h1 == null || h2 == null) {
      return;
    }
    Node n1 = HazelcastTestUtil.getNode(h1);
    Node n2 = HazelcastTestUtil.getNode(h2);
    if (n1 != null && n2 != null) {
      n1.clusterService.removeAddress(n2.address);
      n2.clusterService.removeAddress(n1.address);
    }
  }
}
