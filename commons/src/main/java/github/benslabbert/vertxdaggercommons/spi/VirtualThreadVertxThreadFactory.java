/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.spi;

import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.spi.VertxThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualThreadVertxThreadFactory implements VertxThreadFactory {

  private static final Logger log = LoggerFactory.getLogger(VirtualThreadVertxThreadFactory.class);

  public VirtualThreadVertxThreadFactory() {
    log.info("loading {} SPI", getClass().getCanonicalName());
  }

  @Override
  public void init(VertxBuilder builder) {
    log.info("init spi");

    if (builder.threadFactory() == null) {
      builder.threadFactory(this);
    } else {
      log.warn("a thread factory has already been set!");
    }
  }

  @Override
  public VertxThread newVertxThread(
      Runnable target, String name, boolean worker, long maxExecTime, TimeUnit maxExecTimeUnit) {

    if (worker) {
      return new MyVertxThread(target, name + "-virt", worker, maxExecTime, maxExecTimeUnit);
    }

    // use normal implementation
    return VertxThreadFactory.super.newVertxThread(
        target, name, worker, maxExecTime, maxExecTimeUnit);
  }

  private static class MyVertxThread extends VertxThread {

    private final Thread thread;

    private MyVertxThread(
        Runnable target, String name, boolean worker, long maxExecTime, TimeUnit maxExecTimeUnit) {
      super(target, name, worker, maxExecTime, maxExecTimeUnit);
      this.thread = Thread.ofVirtual().unstarted(target);
      thread.setName(name);
      log.info(
          "created virtual thread name: {} worker? {} maxExecTime {} maxExecTimeUnit {}",
          name,
          worker,
          maxExecTime,
          maxExecTimeUnit);
    }

    @Override
    public long startTime() {
      log.info("startTime");
      return super.startTime();
    }

    @Override
    public boolean isWorker() {
      log.info("isWorker");
      return super.isWorker();
    }

    @Override
    public long maxExecTime() {
      log.info("maxExecTime");
      return super.maxExecTime();
    }

    @Override
    public TimeUnit maxExecTimeUnit() {
      log.info("maxExecTimeUnit");
      return super.maxExecTimeUnit();
    }

    // delegate thread methods to virtual thread
    @Override
    public void start() {
      log.info("start");
      thread.start();
    }

    @Override
    public void run() {
      log.info("run");
      thread.start();
    }

    @Override
    public void interrupt() {
      log.info("interrupt");
      thread.interrupt();
    }

    @Override
    public boolean isInterrupted() {
      log.info("isInterrupted");
      return thread.isInterrupted();
    }

    @Override
    public String toString() {
      log.info("toString");
      return thread.toString();
    }

    @Override
    public ClassLoader getContextClassLoader() {
      log.info("getContextClassLoader");
      return thread.getContextClassLoader();
    }

    @Override
    public void setContextClassLoader(ClassLoader cl) {
      log.info("setContextClassLoader");
      thread.setContextClassLoader(cl);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
      log.info("getStackTrace");
      return thread.getStackTrace();
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
      log.info("getUncaughtExceptionHandler");
      return thread.getUncaughtExceptionHandler();
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler ueh) {
      log.info("setUncaughtExceptionHandler");
      thread.setUncaughtExceptionHandler(ueh);
    }

    // override netty methods
    @Override
    public boolean willCleanupFastThreadLocals() {
      log.info("willCleanupFastThreadLocals");
      boolean superResult = super.willCleanupFastThreadLocals();
      log.info("willCleanupFastThreadLocals ? " + superResult);
      return superResult;
    }

    @Override
    public boolean permitBlockingCalls() {
      log.info("permitBlockingCalls");
      boolean superResult = super.permitBlockingCalls();
      log.info("willCleanupFastThreadLocals ? " + superResult);
      return superResult;
    }
  }
}
