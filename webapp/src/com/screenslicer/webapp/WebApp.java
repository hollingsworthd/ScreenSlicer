/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * ScreenSlicer is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking ScreenSlicer statically or dynamically with other modules is making a combined work
 *   based on ScreenSlicer. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of ScreenSlicer with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on ScreenSlicer. If you modify ScreenSlicer, you may not
 *   extend this exception to your modified version of ScreenSlicer.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
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