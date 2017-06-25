/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 ScreenSlicer committers
 * https://github.com/MachinePublishers/ScreenSlicer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.screenslicer.webapp;

import java.net.URI;
import java.security.SecureRandom;

import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.screenslicer.common.Config;
import com.screenslicer.common.Log;

public class WebApp extends ResourceConfig {

  public static interface Callback {
    void call();
  }

  private static ExceptionListener listener = null;
  private static final Object listenerLock = new Object();
  public static URI INTERNAL_URL;
  public static final boolean DEV = "true".equals(System.getProperty("slicer.dev", "false"));
  public static final boolean DEBUG = "true".equals(System.getProperty("slicer.debug", "false"));
  public static final boolean SANDBOX = "true".equals(System.getProperty("slicer.sandbox", "false"));
  public static final SecureRandom rand = new SecureRandom();
  public static final int THREADS = Integer.parseInt(System.getProperty("slicer.threads", "1"));
  private static final int MAX_REQUESTS = 1048576;

  private WebApp() {}

  public static void main(String[] args) throws Exception {}

  static void exception() {
    synchronized (listenerLock) {
      if (listener != null) {
        listener.exception();
      }
    }
  }

  public static synchronized void start(String name, int port,
      boolean useLoopback, ExceptionListener listener, Callback callback) {
    synchronized (listenerLock) {
      WebApp.listener = listener;
    }
    try {
      Log.init(name, !WebApp.DEV);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
    Config.instance.init();
    try {
      if (DEV || useLoopback) {
        INTERNAL_URL = URI.create("http://127.0.0.1:" + port);
      } else {
        INTERNAL_URL = URI.create("http://0.0.0.0:" + port);
      }
      Log.info("starting");
      if (callback != null) {
        callback.call();
      }
      WebAppConfig config = new WebAppConfig();
      HttpServer httpServer =
          GrizzlyHttpServerFactory.createHttpServer(INTERNAL_URL, config, false);
      CompressionConfig compressionConfig =
          httpServer.getListener("grizzly").getCompressionConfig();
      compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON);
      compressionConfig.setCompressionMinSize(1);
      compressionConfig.setCompressableMimeTypes(config.mimeTypes());
      httpServer.getListener("grizzly").getTransport().
          getWorkerThreadPoolConfig().setMaxPoolSize(MAX_REQUESTS);
      httpServer.getListener("grizzly").getTransport().
          getWorkerThreadPoolConfig().setQueueLimit(MAX_REQUESTS);
      httpServer.getListener("grizzly").getTransport().
          getKernelThreadPoolConfig().setMaxPoolSize(MAX_REQUESTS);
      httpServer.getListener("grizzly").getTransport().
          getKernelThreadPoolConfig().setQueueLimit(MAX_REQUESTS);
      httpServer.start();
      final Object lock = new Object();
      synchronized (lock) {
        lock.wait();
      }
    } catch (Throwable t) {
      Log.exception(t);
      t.printStackTrace();
      System.exit(2);
    }
  }
}