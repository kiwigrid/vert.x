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

package io.vertx.core.http.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.CaOptions;
import io.vertx.core.net.JKSOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.PKCS12Options;
import io.vertx.core.net.TrustStoreOptions;
import io.vertx.core.net.impl.SocketDefaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HttpServerOptionsImpl implements HttpServerOptions {

  private static final int DEFAULT_SENDBUFFERSIZE = -1;
  private static final int DEFAULT_RECEIVEBUFFERSIZE = -1;
  private static final boolean DEFAULT_REUSEADDRESS = true;
  private static final int DEFAULT_TRAFFICCLASS = -1;

  private int sendBufferSize = DEFAULT_SENDBUFFERSIZE;
  private int receiveBufferSize = DEFAULT_RECEIVEBUFFERSIZE;
  private boolean reuseAddress = DEFAULT_REUSEADDRESS;
  private int trafficClass = DEFAULT_TRAFFICCLASS;

  // TCP stuff
  private static SocketDefaults SOCK_DEFAULTS = SocketDefaults.instance;

  private static final boolean DEFAULT_TCPNODELAY = true;
  private static final boolean DEFAULT_TCPKEEPALIVE = SOCK_DEFAULTS.isTcpKeepAlive();
  private static final int DEFAULT_SOLINGER = SOCK_DEFAULTS.getSoLinger();

  private boolean tcpNoDelay = DEFAULT_TCPNODELAY;
  private boolean tcpKeepAlive = DEFAULT_TCPKEEPALIVE;
  private int soLinger = DEFAULT_SOLINGER;
  private boolean usePooledBuffers;
  private int idleTimeout;

  // SSL stuff

  private boolean ssl;
  private KeyStoreOptions keyStore;
  private TrustStoreOptions trustStore;
  private Set<String> enabledCipherSuites = new HashSet<>();

  // Server specific HTTP stuff

  private static final String DEFAULT_HOST = "0.0.0.0";
  private static final int DEFAULT_ACCEPT_BACKLOG = 1024;

  private String host;
  private int acceptBacklog;

  // Server specific SSL stuff

  private boolean clientAuthRequired;
  private ArrayList<String> crlPaths;
  private ArrayList<Buffer> crlValues;

  // Server specific HTTP stuff

  private static final int DEFAULT_MAXWEBSOCKETFRAMESIZE = 65536;
  private static final int DEFAULT_PORT = 80;  // Default port is 80 for HTTP not 0 from NetServerOptions

  private boolean compressionSupported;
  private int maxWebsocketFrameSize;
  private Set<String> websocketSubProtocols = new HashSet<>();
  private int port;

  HttpServerOptionsImpl(HttpServerOptions other) {
    this.sendBufferSize = other.getSendBufferSize();
    this.receiveBufferSize = other.getReceiveBufferSize();
    this.reuseAddress = other.isReuseAddress();
    this.trafficClass = other.getTrafficClass();
    this.tcpNoDelay = other.isTcpNoDelay();
    this.tcpKeepAlive = other.isTcpKeepAlive();
    this.soLinger = other.getSoLinger();
    this.usePooledBuffers = other.isUsePooledBuffers();
    this.idleTimeout = other.getIdleTimeout();
    this.ssl = other.isSsl();
    this.keyStore = other.getKeyStoreOptions() != null ? other.getKeyStoreOptions().clone() : null;
    this.trustStore = other.getTrustStoreOptions() != null ? other.getTrustStoreOptions().clone() : null;
    this.enabledCipherSuites = other.getEnabledCipherSuites() == null ? null : new HashSet<>(other.getEnabledCipherSuites());
    this.port = other.getPort();
    this.host = other.getHost();
    this.acceptBacklog = other.getAcceptBacklog();
    this.crlPaths = other.getCrlPaths() != null ? new ArrayList<>(other.getCrlPaths()) : null;
    this.crlValues = other.getCrlValues() != null ? new ArrayList<>(other.getCrlValues()) : null;
    this.compressionSupported = other.isCompressionSupported();
    this.maxWebsocketFrameSize = other.getMaxWebsocketFrameSize();
    this.websocketSubProtocols = other.getWebsocketSubProtocols() != null ? new HashSet<>(other.getWebsocketSubProtocols()) : null;
    this.port = other.getPort();
  }

  HttpServerOptionsImpl(JsonObject json) {
    this.sendBufferSize = json.getInteger("sendBufferSize", DEFAULT_SENDBUFFERSIZE);
    this.receiveBufferSize = json.getInteger("receiveBufferSize", DEFAULT_RECEIVEBUFFERSIZE);
    this.reuseAddress = json.getBoolean("reuseAddress", DEFAULT_REUSEADDRESS);
    this.trafficClass = json.getInteger("trafficClass", DEFAULT_TRAFFICCLASS);
    this.tcpNoDelay = json.getBoolean("tcpNoDelay", DEFAULT_TCPNODELAY);
    this.tcpKeepAlive = json.getBoolean("tcpKeepAlive", DEFAULT_TCPKEEPALIVE);
    this.soLinger = json.getInteger("soLinger", DEFAULT_SOLINGER);
    this.usePooledBuffers = json.getBoolean("usePooledBuffers", false);
    this.idleTimeout = json.getInteger("idleTimeout", 0);
    this.ssl = json.getBoolean("ssl", false);
    JsonObject keyStoreJson = json.getObject("keyStoreOptions");
    if (keyStoreJson != null) {
      String type = keyStoreJson.getString("type", null);
      switch (type != null ? type.toLowerCase() : "jks") {
        case "jks":
          keyStore = JKSOptions.optionsFromJson(keyStoreJson);
          break;
        case "pkcs12":
          keyStore = PKCS12Options.optionsFromJson(keyStoreJson);
          break;
        case "keycert":
          keyStore = KeyCertOptions.optionsFromJson(keyStoreJson);
          break;
        default:
          throw new IllegalArgumentException("Invalid key store type: " + type);
      }
    }
    JsonObject trustStoreJson = json.getObject("trustStoreOptions");
    if (trustStoreJson != null) {
      String type = trustStoreJson.getString("type", null);
      switch (type != null ? type.toLowerCase() : "jks") {
        case "jks":
          trustStore = JKSOptions.optionsFromJson(trustStoreJson);
          break;
        case "pkcs12":
          trustStore = PKCS12Options.optionsFromJson(trustStoreJson);
          break;
        case "ca":
          trustStore = CaOptions.optionsFromJson(trustStoreJson);
          break;
        default:
          throw new IllegalArgumentException("Invalid trust store type: " + type);
      }
    }
    JsonArray arr = json.getArray("enabledCipherSuites");
    this.enabledCipherSuites = arr == null ? null : new HashSet<String>(arr.toList());
    this.port = json.getInteger("port", DEFAULT_PORT);
    this.host = json.getString("host", DEFAULT_HOST);
    this.acceptBacklog = json.getInteger("acceptBacklog", DEFAULT_ACCEPT_BACKLOG);
    arr = json.getArray("crlPaths");
    this.crlPaths = arr == null ? new ArrayList<>() : new ArrayList<String>(arr.toList());
    this.crlValues = new ArrayList<>();
    arr = json.getArray("crlValues");
    if (arr != null) {
      ((List<byte[]>) arr.toList()).stream().map(Buffer::buffer).forEach(crlValues::add);
    }
    this.compressionSupported = json.getBoolean("compressionSupported", false);
    this.maxWebsocketFrameSize = json.getInteger("maxWebsocketFrameSize", DEFAULT_MAXWEBSOCKETFRAMESIZE);
    arr = json.getArray("websocketSubProtocols");
    this.websocketSubProtocols = new HashSet<>();
    if (arr != null) {
      websocketSubProtocols.addAll(arr.toList());
    }
    this.port = json.getInteger("port", DEFAULT_PORT);
  }

  HttpServerOptionsImpl() {
    sendBufferSize = DEFAULT_SENDBUFFERSIZE;
    receiveBufferSize = DEFAULT_RECEIVEBUFFERSIZE;
    reuseAddress = DEFAULT_REUSEADDRESS;
    trafficClass = DEFAULT_TRAFFICCLASS;
    tcpNoDelay = DEFAULT_TCPNODELAY;
    tcpKeepAlive = DEFAULT_TCPKEEPALIVE;
    soLinger = DEFAULT_SOLINGER;
    port = DEFAULT_PORT;
    host = DEFAULT_HOST;
    acceptBacklog = DEFAULT_ACCEPT_BACKLOG;
    crlPaths = new ArrayList<>();
    crlValues = new ArrayList<>();
    maxWebsocketFrameSize = DEFAULT_MAXWEBSOCKETFRAMESIZE;
    port = DEFAULT_PORT;
  }

  @Override
  public int getSendBufferSize() {
    return sendBufferSize;
  }

  @Override
  public HttpServerOptions setSendBufferSize(int sendBufferSize) {
    if (sendBufferSize < 1) {
      throw new IllegalArgumentException("sendBufferSize must be > 0");
    }
    this.sendBufferSize = sendBufferSize;
    return this;
  }

  @Override
  public int getReceiveBufferSize() {
    return receiveBufferSize;
  }

  @Override
  public HttpServerOptions setReceiveBufferSize(int receiveBufferSize) {
    if (receiveBufferSize < 1) {
      throw new IllegalArgumentException("receiveBufferSize must be > 0");
    }
    this.receiveBufferSize = receiveBufferSize;
    return this;
  }

  @Override
  public boolean isReuseAddress() {
    return reuseAddress;
  }

  @Override
  public HttpServerOptions setReuseAddress(boolean reuseAddress) {
    this.reuseAddress = reuseAddress;
    return this;
  }

  @Override
  public int getTrafficClass() {
    return trafficClass;
  }

  @Override
  public HttpServerOptions setTrafficClass(int trafficClass) {
    if (trafficClass < 0 || trafficClass > 255) {
      throw new IllegalArgumentException("trafficClass tc must be 0 <= tc <= 255");
    }
    this.trafficClass = trafficClass;
    return this;
  }

  @Override
  public boolean isTcpNoDelay() {
    return tcpNoDelay;
  }

  @Override
  public HttpServerOptions setTcpNoDelay(boolean tcpNoDelay) {
    this.tcpNoDelay = tcpNoDelay;
    return this;
  }

  @Override
  public boolean isTcpKeepAlive() {
    return tcpKeepAlive;
  }

  @Override
  public HttpServerOptions setTcpKeepAlive(boolean tcpKeepAlive) {
    this.tcpKeepAlive = tcpKeepAlive;
    return this;
  }

  @Override
  public int getSoLinger() {
    return soLinger;
  }

  @Override
  public HttpServerOptions setSoLinger(int soLinger) {
    if (soLinger < 0) {
      throw new IllegalArgumentException("soLinger must be >= 0");
    }
    this.soLinger = soLinger;
    return this;
  }

  @Override
  public boolean isUsePooledBuffers() {
    return usePooledBuffers;
  }

  @Override
  public HttpServerOptions setUsePooledBuffers(boolean usePooledBuffers) {
    this.usePooledBuffers = usePooledBuffers;
    return this;
  }

  @Override
  public HttpServerOptions setIdleTimeout(int idleTimeout) {
    if (idleTimeout < 0) {
      throw new IllegalArgumentException("idleTimeout must be >= 0");
    }
    this.idleTimeout = idleTimeout;
    return this;
  }

  @Override
  public int getIdleTimeout() {
    return idleTimeout;
  }

  @Override
  public boolean isSsl() {
    return ssl;
  }

  @Override
  public HttpServerOptions setSsl(boolean ssl) {
    this.ssl = ssl;
    return this;
  }

  @Override
  public KeyStoreOptions getKeyStoreOptions() {
    return keyStore;
  }

  @Override
  public HttpServerOptions setKeyStoreOptions(KeyStoreOptions keyStore) {
    this.keyStore = keyStore;
    return this;
  }

  @Override
  public TrustStoreOptions getTrustStoreOptions() {
    return trustStore;
  }

  @Override
  public HttpServerOptions setTrustStoreOptions(TrustStoreOptions trustStore) {
    this.trustStore = trustStore;
    return this;
  }

  @Override
  public HttpServerOptions addEnabledCipherSuite(String suite) {
    enabledCipherSuites.add(suite);
    return this;
  }

  @Override
  public Set<String> getEnabledCipherSuites() {
    return enabledCipherSuites;
  }


  @Override
  public boolean isClientAuthRequired() {
    return clientAuthRequired;
  }

  @Override
  public HttpServerOptions setClientAuthRequired(boolean clientAuthRequired) {
    this.clientAuthRequired = clientAuthRequired;
    return this;
  }

  @Override
  public List<String> getCrlPaths() {
    return crlPaths;
  }

  @Override
  public HttpServerOptions addCrlPath(String crlPath) throws NullPointerException {
    if (crlPath == null) {
      throw new NullPointerException("No null crl accepted");
    }
    crlPaths.add(crlPath);
    return this;
  }

  @Override
  public List<Buffer> getCrlValues() {
    return crlValues;
  }

  @Override
  public HttpServerOptions addCrlValue(Buffer crlValue) throws NullPointerException {
    if (crlValue == null) {
      throw new NullPointerException("No null crl accepted");
    }
    crlValues.add(crlValue);
    return this;
  }

  @Override
  public int getAcceptBacklog() {
    return acceptBacklog;
  }

  @Override
  public HttpServerOptions setAcceptBacklog(int acceptBacklog) {
    this.acceptBacklog = acceptBacklog;
    return this;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public HttpServerOptions setPort(int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException("port p must be in range 0 <= p <= 65535");
    }
    this.port = port;
    return this;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public HttpServerOptions setHost(String host) {
    this.host = host;
    return this;
  }

  @Override
  public boolean isCompressionSupported() {
    return compressionSupported;
  }

  @Override
  public HttpServerOptions setCompressionSupported(boolean compressionSupported) {
    this.compressionSupported = compressionSupported;
    return this;
  }

  @Override
  public int getMaxWebsocketFrameSize() {
    return maxWebsocketFrameSize;
  }

  @Override
  public HttpServerOptions setMaxWebsocketFrameSize(int maxWebsocketFrameSize) {
    this.maxWebsocketFrameSize = maxWebsocketFrameSize;
    return this;
  }

  @Override
  public HttpServerOptions addWebsocketSubProtocol(String subProtocol) {
    websocketSubProtocols.add(subProtocol);
    return this;
  }

  @Override
  public Set<String> getWebsocketSubProtocols() {
    return websocketSubProtocols;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HttpServerOptionsImpl that = (HttpServerOptionsImpl) o;

    if (acceptBacklog != that.acceptBacklog) return false;
    if (clientAuthRequired != that.clientAuthRequired) return false;
    if (compressionSupported != that.compressionSupported) return false;
    if (idleTimeout != that.idleTimeout) return false;
    if (maxWebsocketFrameSize != that.maxWebsocketFrameSize) return false;
    if (port != that.port) return false;
    if (receiveBufferSize != that.receiveBufferSize) return false;
    if (reuseAddress != that.reuseAddress) return false;
    if (sendBufferSize != that.sendBufferSize) return false;
    if (soLinger != that.soLinger) return false;
    if (ssl != that.ssl) return false;
    if (tcpKeepAlive != that.tcpKeepAlive) return false;
    if (tcpNoDelay != that.tcpNoDelay) return false;
    if (trafficClass != that.trafficClass) return false;
    if (usePooledBuffers != that.usePooledBuffers) return false;
    if (crlPaths != null ? !crlPaths.equals(that.crlPaths) : that.crlPaths != null) return false;
    if (crlValues != null ? !crlValues.equals(that.crlValues) : that.crlValues != null) return false;
    if (enabledCipherSuites != null ? !enabledCipherSuites.equals(that.enabledCipherSuites) : that.enabledCipherSuites != null)
      return false;
    if (host != null ? !host.equals(that.host) : that.host != null) return false;
    if (keyStore != null ? !keyStore.equals(that.keyStore) : that.keyStore != null) return false;
    if (trustStore != null ? !trustStore.equals(that.trustStore) : that.trustStore != null) return false;
    if (websocketSubProtocols != null ? !websocketSubProtocols.equals(that.websocketSubProtocols) : that.websocketSubProtocols != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sendBufferSize;
    result = 31 * result + receiveBufferSize;
    result = 31 * result + (reuseAddress ? 1 : 0);
    result = 31 * result + trafficClass;
    result = 31 * result + (tcpNoDelay ? 1 : 0);
    result = 31 * result + (tcpKeepAlive ? 1 : 0);
    result = 31 * result + soLinger;
    result = 31 * result + (usePooledBuffers ? 1 : 0);
    result = 31 * result + idleTimeout;
    result = 31 * result + (ssl ? 1 : 0);
    result = 31 * result + (keyStore != null ? keyStore.hashCode() : 0);
    result = 31 * result + (trustStore != null ? trustStore.hashCode() : 0);
    result = 31 * result + (enabledCipherSuites != null ? enabledCipherSuites.hashCode() : 0);
    result = 31 * result + (host != null ? host.hashCode() : 0);
    result = 31 * result + acceptBacklog;
    result = 31 * result + (clientAuthRequired ? 1 : 0);
    result = 31 * result + (crlPaths != null ? crlPaths.hashCode() : 0);
    result = 31 * result + (crlValues != null ? crlValues.hashCode() : 0);
    result = 31 * result + (compressionSupported ? 1 : 0);
    result = 31 * result + maxWebsocketFrameSize;
    result = 31 * result + (websocketSubProtocols != null ? websocketSubProtocols.hashCode() : 0);
    result = 31 * result + port;
    return result;
  }
}
