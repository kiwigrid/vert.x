package org.vertx.java.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.datagram.InternetProtocolFamily;
import org.vertx.java.core.dns.DnsClient;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.WebSocketFrame;
import org.vertx.java.core.http.impl.ws.DefaultWebSocketFrame;
import org.vertx.java.core.http.impl.ws.WebSocketFrameInternal;
import org.vertx.java.core.impl.DefaultContext;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.impl.EventLoopContext;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.impl.ConnectionBase;
import org.vertx.java.core.net.impl.DefaultNetServer;
import org.vertx.java.core.net.impl.ServerID;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.spi.Action;
import org.vertx.java.core.spi.cluster.ClusterManager;

import static org.junit.Assert.*;

public class WebSocketImplBaseTest {

  /**
   * 
   * Testing plan:
   * 
   * === handleFrame ===
   * 
   * assumptions: frames are coming in the correct order, i.e. TEXT/BINARY is
   * followed by a CONTINUATION (if needed); no CONTINUATION without appropriate
   * TEXT/BINARY or another CONTINNUATION frames comes; no CONTINUATION directly
   * after a final frame comes. All these assumptions are assured by the Netty
   * WebSocketFrameDecoder
   * 
   * Note: only processing for the dataHandler is tested since the code for the
   * frameHandler code has not been changed
   * 
   * - an empty frame message -> handle on the dataHandler is invoked with an
   * empty buffer
   * 
   * - one final frame message -> handle on the dataHandler is invoked
   * 
   * - several final frame messages -> handle on the dataHandler is invoked once
   * per message
   * 
   * - one not final frame -> handle on the dataHandler is not invoked
   * 
   * - one multi-frame message is sent -> handle on the dataHandler is invoked
   * with the complete data
   * 
   * - several multi-frame messages are sent -> handle on the dataHandler is
   * invoked once per complete message
   * 
   * Sending message:
   * 
   * - sending null message ->
   * 
   * - sending an empty binary message ->
   * 
   * - sending one small binary message ->
   * 
   * - sending several small binary message ->
   * 
   * - sending one large binary messages ->
   * 
   * - sending several large binary messages ->
   * 
   */

  private TestWebSocketImplBase webSocketImplBase;

  @Before
  public void init() {
    webSocketImplBase = new TestWebSocketImplBase(new MockConnection());
    webSocketImplBase.dataHandler = new MockDataHandler();
    webSocketImplBase.closed = false;
  }

  @Test
  public void testHandleFrame_emptyBinaryFrame() {
    WebSocketFrameInternal frame = new DefaultWebSocketFrame(WebSocketFrame.FrameType.BINARY);
    webSocketImplBase.handleFrame(frame);
    assertTrue(((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.size() == 1);
    assertEquals("", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(0).toString());
  }

  @Test
  public void testHandleFrame_finalTextFrame() {
    WebSocketFrameInternal frame = new DefaultWebSocketFrame("test_frame");
    webSocketImplBase.handleFrame(frame);
    assertTrue(((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.size() == 1);
    assertEquals("test_frame", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(0).toString());
  }

  @Test
  public void testHandleFrame_severalFinalBinaryFrame() {
    WebSocketFrameInternal frame0 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.BINARY, Unpooled.copiedBuffer("test_frame_0".getBytes()));
    webSocketImplBase.handleFrame(frame0);
    WebSocketFrameInternal frame1 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.BINARY, Unpooled.copiedBuffer("test_frame_1".getBytes()));
    webSocketImplBase.handleFrame(frame1);
    assertTrue(((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.size() == 2);
    assertEquals("test_frame_0", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(0));
    assertEquals("test_frame_1", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(1));
  }

  @Test
  public void testHandleFrame_oneNotFinalTextFrame() {
    WebSocketFrameInternal frame0 = new DefaultWebSocketFrame("test_frame_0", false);
    webSocketImplBase.handleFrame(frame0);
    assertTrue(((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.size() == 0);
  }

  @Test
  public void testHandleFrame_multiFrameBinaryMessage() {
    WebSocketFrameInternal frame0 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.BINARY, Unpooled.copiedBuffer("test_frame_0".getBytes()),
        false);
    webSocketImplBase.handleFrame(frame0);
    WebSocketFrameInternal frame1 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.CONTINUATION,
        Unpooled.copiedBuffer("test_frame_1".getBytes()), false);
    webSocketImplBase.handleFrame(frame1);
    WebSocketFrameInternal frame2 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.CONTINUATION,
        Unpooled.copiedBuffer("test_frame_2".getBytes()), true);
    webSocketImplBase.handleFrame(frame2);
    assertTrue(((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.size() == 1);
    assertEquals("test_frame_0test_frame_1test_frame_2", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(0));
  }

  @Test
  public void testHandleFrame_twoMultiFrameTextMessages() {
    WebSocketFrameInternal frame00 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.BINARY, Unpooled.copiedBuffer("test_frame_00".getBytes()),
        false);
    webSocketImplBase.handleFrame(frame00);
    WebSocketFrameInternal frame01 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.CONTINUATION, Unpooled.copiedBuffer("test_frame_01"
        .getBytes()), false);
    webSocketImplBase.handleFrame(frame01);
    WebSocketFrameInternal frame02 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.CONTINUATION, Unpooled.copiedBuffer("test_frame_02"
        .getBytes()), true);
    webSocketImplBase.handleFrame(frame02);
    WebSocketFrameInternal frame10 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.BINARY, Unpooled.copiedBuffer("test_frame_10".getBytes()),
        false);
    webSocketImplBase.handleFrame(frame10);
    WebSocketFrameInternal frame11 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.CONTINUATION, Unpooled.copiedBuffer("test_frame_11"
        .getBytes()), false);
    webSocketImplBase.handleFrame(frame11);
    WebSocketFrameInternal frame12 = new DefaultWebSocketFrame(WebSocketFrame.FrameType.CONTINUATION, Unpooled.copiedBuffer("test_frame_12"
        .getBytes()), true);
    webSocketImplBase.handleFrame(frame12);
    assertEquals(2, ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.size());
    assertEquals("test_frame_00test_frame_01test_frame_02", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(0));
    assertEquals("test_frame_10test_frame_11test_frame_12", ((MockDataHandler) webSocketImplBase.dataHandler).receivedMessages.get(1));
  }

  @Test
  public void testWriteTextFrameInternal_oneSmallMessage() throws UnsupportedEncodingException {
    webSocketImplBase.writeTextFrameInternal("test_frameä");
    assertEquals(1, ((MockConnection) webSocketImplBase.conn).writtenObjects.size());
    DefaultWebSocketFrame wsFrame = (DefaultWebSocketFrame) ((MockConnection) webSocketImplBase.conn).writtenObjects.get(0);
    assertEquals("test_frameä", wsFrame.textData());
  }

  private static class MockDataHandler implements Handler<Buffer> {

    List<String> receivedMessages = new ArrayList<>();

    @Override
    public void handle(Buffer event) {
      receivedMessages.add(event.toString());
    }

  }

  private static class TestWebSocketImplBase extends WebSocketImplBase<Object> {

    protected TestWebSocketImplBase(MockConnection mockConnection) {
      super(new DefaultVertx(), mockConnection);
    }

    @Override
    public Object writeBinaryFrame(Buffer data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object writeTextFrame(String str) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object closeHandler(Handler<Void> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object frameHandler(Handler<WebSocketFrame> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object endHandler(Handler<Void> endHandler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object dataHandler(Handler<Buffer> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object pause() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object resume() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object exceptionHandler(Handler<Throwable> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object write(Buffer data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object setWriteQueueMaxSize(int maxSize) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object drainHandler(Handler<Void> handler) {
      throw new UnsupportedOperationException();
    }

  }

  private static class MockConnection extends ConnectionBase {

    List<Object> writtenObjects = new ArrayList<>();

    protected MockConnection() {
      super(null, null, null);
    }

    @Override
    protected void handleInterestedOpsChanged() {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture write(Object obj) {
      writtenObjects.add(obj);
      return null;
    }

  }

}
