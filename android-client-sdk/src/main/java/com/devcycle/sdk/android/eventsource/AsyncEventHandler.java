package com.devcycle.sdk.android.eventsource;

import com.devcycle.sdk.android.util.DevCycleLogger;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Adapted from https://github.com/aslakhellesoy/eventsource-java/blob/master/src/main/java/com/github/eventsource/client/impl/AsyncEventSourceHandler.java
 * <p>
 * We use this in conjunction with a <i>single-threaded</i> executor to ensure that messages are handled
 * on a worker thread in the same order that they were received.
 * <p>
 * This class guarantees that runtime exceptions are never thrown back to the EventSource. 
 */
final class AsyncEventHandler implements EventHandler {
  private final Executor executor;
  private final EventHandler eventSourceHandler;
  final Semaphore semaphore; // visible for tests

  AsyncEventHandler(Executor executor, EventHandler eventSourceHandler, Semaphore semaphore) {
    this.executor = executor;
    this.eventSourceHandler = eventSourceHandler;
    this.semaphore = semaphore;
  }

  public void onOpen() {
    execute(() -> {
      try {
        eventSourceHandler.onOpen();
      } catch (Exception e) {
        handleUnexpectedError(e);
      }
    });
  }

  public void onClosed() {
    execute(() -> {
      try {
        eventSourceHandler.onClosed();
      } catch (Exception e) {
        handleUnexpectedError(e);
      }
    });
  }

  public void onComment(final String comment) {
    execute(() -> {
      try {
        eventSourceHandler.onComment(comment);
      } catch (Exception e) {
        handleUnexpectedError(e);
      }
    });
  }

  public void onMessage(final String event, final MessageEvent messageEvent) {
    execute(() -> {
      try {
        eventSourceHandler.onMessage(event, messageEvent);
      } catch (Exception e) {
        handleUnexpectedError(e);
      } finally {
        messageEvent.close();
      }
    });
  }

  public void onError(final Throwable error) {
    execute(() -> {
      onErrorInternal(error);
    });
  }

  private void handleUnexpectedError(Throwable error) {
    DevCycleLogger.w("Caught unexpected error from EventHandler: " + error.toString());
    DevCycleLogger.d("Stack trace: %s", new LazyStackTrace(error));
    onErrorInternal(error);
  }

  private void onErrorInternal(Throwable error) {
    try {
      eventSourceHandler.onError(error);
    } catch (Throwable errorFromErrorHandler) {
      DevCycleLogger.w("Caught unexpected error from EventHandler.onError(): " + errorFromErrorHandler.toString());
      DevCycleLogger.d("Stack trace: %s", new LazyStackTrace(error));
    }
  }

  private void execute(Runnable task) {
    acquire();
    try {
      executor.execute(() -> {
        try {
          task.run();
        } finally {
          release();
        }
      });
    } catch (Exception e) { // COVERAGE: this condition can't be reproduced in unit tests
      // probably a RejectedExecutionException due to pool shutdown
      release();
      throw e;
    }
  }

  private void acquire() {
    if (semaphore != null) {
      try {
        semaphore.acquire();
      } catch (InterruptedException e) { // COVERAGE: this condition can't be reproduced in unit tests
        throw new RejectedExecutionException("Thread interrupted while waiting for event thread semaphore", e);
      }
    }
  }

  private void release() {
    if (semaphore != null) {
      semaphore.release();
    }
  }
}