/*
 * Copyright 2014 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version 2.0
 *   (the "License"); you may not use this file except in compliance with the
 *   License.  You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 */

package io.vertx.core.eventbus.impl.codecs;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class DoubleMessageCodec implements MessageCodec<Double, Double> {

  @Override
  public void encodeToWire(Buffer buffer, Double d) {
    buffer.appendDouble(d);
  }

  @Override
  public Double decodeFromWire(int pos, Buffer buffer) {
    return buffer.getDouble(pos);
  }

  @Override
  public Double transform(Double d) {
    // Doubles are immutable so just return it
    return d;
  }

  @Override
  public String name() {
    return "double";
  }

  @Override
  public byte systemCodecID() {
    return 6;
  }
}
