/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * version 3 along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 */
package com.screenslicer.core.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.glassfish.grizzly.http.server.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.util.Util;
import com.screenslicer.webapp.WebApp;
import com.screenslicer.webapp.WebResource;

@Path("/httpstatus")
public class HttpStatus implements WebResource {
  private static int status = 0;
  private static int prevLen = 0;
  private static int len = 0;
  private static final Object lock = new Object();
  private static final int SLEEP = 1000;
  private static final int TIMEOUT = 25 * 1000;
  private static final int MIN_LEN = 500;

  private static boolean hasContent(RemoteWebDriver driver, String src) {
    if (CommonUtil.isEmpty(src)) {
      return false;
    }
    final boolean[] content = new boolean[1];
    try {
      Element element = null;
      if (driver == null) {
        element = Jsoup.parse(src);
      } else {
        element = Util.openElement(driver, null, null, null);
      }
      if (element == null) {
        return false;
      }
      element.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int d) {}

        @Override
        public void head(Node n, int d) {
          if (!content[0] && !Util.isEmpty(n)) {
            content[0] = true;
          }
        }
      });
    } catch (Throwable t) {
      return false;
    }
    return content[0];
  }

  public static int status(RemoteWebDriver driver, boolean wait) {
    if (WebApp.DEBUG) {
      System.out.println("check status...");
    }
    int totalWait = 0;
    String src = null;
    while (totalWait < TIMEOUT) {
      src = null;
      try {
        src = driver.getPageSource();
        if (!CommonUtil.isEmpty(src)) {
          synchronized (lock) {
            prevLen = len;
            len = src.length();
          }
        }
      } catch (Throwable t) {
        synchronized (lock) {
          src = null;
          prevLen = 0;
          len = 0;
        }
        Log.exception(t);
      }
      if (WebApp.DEBUG) {
        synchronized (lock) {
          System.out.println("status=" + status);
        }
      }
      synchronized (lock) {
        if (status != 0) {
          break;
        }
      }
      boolean validLen;
      synchronized (lock) {
        validLen = prevLen == len && len > MIN_LEN;
      }
      if (validLen && hasContent(null, src)) {
        try {
          driver.getKeyboard().sendKeys(Keys.ESCAPE);
        } catch (Throwable t) {
          Log.exception(t);
        }
        Util.driverSleepVeryShort();
        if (!hasContent(driver, src)) {
          synchronized (lock) {
            status = 204;
          }
        }
        break;
      }
      if (wait) {
        if (WebApp.DEBUG) {
          System.out.println("waiting for status...");
        }
        try {
          Thread.sleep(SLEEP);
        } catch (InterruptedException e) {
          Log.exception(e);
        }
        totalWait += SLEEP;
      } else {
        break;
      }
    }
    int myStatus;
    synchronized (lock) {
      myStatus = status;
      status = -1;
    }
    return myStatus;
  }

  @Path("/{statuscode}")
  @Produces("text/html")
  @GET
  public static String execute(@PathParam("statuscode") String statusCode, @Context Request req) {
    if ("127.0.0.1".equals(req.getRemoteAddr())) {
      int newStatus = Integer.parseInt(statusCode);
      synchronized (lock) {
        if (status <= 0 || newStatus == 0) {
          status = newStatus;
          prevLen = 0;
          len = 0;
        }
      }
      if (WebApp.DEBUG) {
        System.out.println("Http status: " + newStatus);
      }
    }
    return "";
  }
}
