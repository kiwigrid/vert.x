package org.vertx.java.spi.cluster.impl.hazelcast;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

/**
 * @author <a href="mailto:andreas.berger@kiwigrid.com">Andreas Berger</a>
 */
public class MultiMapEntryProcessor<K, V> implements EntryProcessor<K, Set<V>>, DataSerializable {
  private V val;
  private boolean add = true;

  public MultiMapEntryProcessor() {
  }

  public MultiMapEntryProcessor(V val, boolean add) {
    this.val = val;
    this.add = add;
  }

  @Override
  public Object process(Map.Entry<K, Set<V>> entry) {
    if (entry == null) {
      return null;
    }
    Set<V> values = entry.getValue();
    boolean changed = false;
    if (add) {
      if (values == null) {
        values = new HashSet<>();
      }
      changed = values.add(val);
    } else if (values != null) {
      changed = values.remove(val);
      if (values.isEmpty()) {
        values = null;
      }
    }
    if (changed) {
      entry.setValue(values);
    }
    return null;
  }

  @Override
  public EntryBackupProcessor<K, Set<V>> getBackupProcessor() {
    return new MultiMapEntryBackupProcessor<>(val, add);
  }

  @Override
  public void writeData(ObjectDataOutput out) throws IOException {
    out.writeObject(val);
    out.writeBoolean(add);
  }

  @Override
  public void readData(ObjectDataInput in) throws IOException {
    val = in.readObject();
    add = in.readBoolean();
  }

  public static class MultiMapEntryBackupProcessor<K, V> extends MultiMapEntryProcessor<K, V>
          implements EntryBackupProcessor<K, Set<V>> {

    public MultiMapEntryBackupProcessor() {
    }

    public MultiMapEntryBackupProcessor(V val, boolean add) {
      super(val, add);
    }

    @Override
    public void processBackup(Map.Entry<K, Set<V>> entry) {
      process(entry);
    }
  }
}
