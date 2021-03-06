/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

class FutureImpl<T> implements Future<T> {
  private boolean failed;
  private boolean succeeded;
  private Handler<AsyncResult<T>> handler;
  private T result;
  private Throwable throwable;

  /**
   * Create a FutureResult that hasn't completed yet
   */
  FutureImpl() {
  }

  /**
   * Create a VoidResult that has already completed
   * @param t The Throwable or null if succeeded
   */
  FutureImpl(Throwable t) {
    if (t == null) {
      setResult(null);
    } else {
      setFailure(t);
    }
  }

  /**
   * Create a FutureResult that has already succeeded
   * @param result The result
   */
  FutureImpl(T result) {
    setResult(result);
  }

  /**
   * The result of the operation. This will be null if the operation failed.
   */
  public T result() {
    return result;
  }

  /**
   * An exception describing failure. This will be null if the operation succeeded.
   */
  public Throwable cause() {
    return throwable;
  }

  /**
   * Did it succeeed?
   */
  public boolean succeeded() {
    return succeeded;
  }

  /**
   * Did it fail?
   */
  public boolean failed() {
    return failed;
  }

  /**
   * Has it completed?
   */
  public boolean complete() {
    return failed || succeeded;
  }

  /**
   * Set a handler for the result. It will get called when it's complete
   */
  public FutureImpl<T> setHandler(Handler<AsyncResult<T>> handler) {
    this.handler = handler;
    checkCallHandler();
    return this;
  }

  /**
   * Set the result. Any handler will be called, if there is one
   */
  public FutureImpl<T> setResult(T result) {
    if (this.succeeded || this.failed) {
      throw new IllegalStateException("Result has already been set");
    }
    this.result = result;
    succeeded = true;
    checkCallHandler();
    return this;
  }

  /**
   * Set the failure. Any handler will be called, if there is one
   */
  public FutureImpl<T> setFailure(Throwable throwable) {
    if (this.succeeded || this.failed) {
      throw new IllegalStateException("Failure has already been set");
    }
    this.throwable = throwable;
    failed = true;
    checkCallHandler();
    return this;
  }

  private void checkCallHandler() {
    if (handler != null && complete()) {
      handler.handle(this);
    }
  }
}
