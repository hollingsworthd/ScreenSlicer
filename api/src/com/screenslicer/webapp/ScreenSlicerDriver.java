/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License version 3
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
package com.screenslicer.webapp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.screenslicer.api.datatype.Contact;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.Result;
import com.screenslicer.api.request.Cancel;
import com.screenslicer.api.request.EmailExport;
import com.screenslicer.api.request.Extract;
import com.screenslicer.api.request.Fetch;
import com.screenslicer.api.request.FormLoad;
import com.screenslicer.api.request.FormQuery;
import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.api.request.Request;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;

@Path("/screenslicer")
public class ScreenSlicerDriver implements WebResource {
  private static final Collection<String> cancelledJobs = new HashSet<String>();
  private static final Object cancelledLock = new Object();
  private static final long WAIT = 2000;
  private static final Object doneMapLock = new Object();
  private static Map<String, AtomicBoolean> doneMap = new HashMap<String, AtomicBoolean>();
  private static AtomicLong latestThread = new AtomicLong();
  private static AtomicLong curThread = new AtomicLong();

  /**
   * @Deprecated This class will be renamed in version 2.0.0
   */
  public static void main(String[] args) throws Exception {
    WebApp.start("driver", 8887, true, null, null);
  }

  public static final boolean isCancelled(String runGuid) {
    if (!CommonUtil.isEmpty(runGuid)) {
      synchronized (cancelledLock) {
        return cancelledJobs.contains(runGuid);
      }
    }
    return false;
  }

  @Path("cancel")
  @POST
  @Consumes("application/json")
  public static final String cancel(String reqString) {
    ScreenSlicer.cancel(Cancel.instance(reqString));
    return "";
  }

  @Path("is-busy/{instanceIp}")
  @GET
  @Produces("text/plain; charset=utf-8")
  public static final String isBusy(@PathParam("instanceIp") String instanceIp) {
    return Boolean.toString(ScreenSlicer.isBusy(instanceIp));
  }

  @Path("export")
  @POST
  @Consumes("application/json")
  public static final String export(final String reqString) {
    process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        ScreenSlicer.export(request, EmailExport.instance(reqString));
        return null;
      }
    });
    return "";
  }

  @Path("query-form")
  @POST
  @Consumes("application/json")
  @Produces("application/json; charset=utf-8")
  public static final String queryForm(final String reqString) {
    return process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        return Result.toJson(ScreenSlicer.queryForm(request, FormQuery.instance(reqString)));
      }
    });
  }

  @Path("query-keyword")
  @POST
  @Consumes("application/json")
  @Produces("application/json; charset=utf-8")
  public static final String queryKeyword(final String reqString) {
    return process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        return Result.toJson(ScreenSlicer.queryKeyword(request, KeywordQuery.instance(reqString)));
      }
    });
  }

  @Path("expand-search-result")
  @POST
  @Consumes("application/json")
  @Produces("application/json; charset=utf-8")
  public static final String expandSearchResult(final String reqString) {
    return process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        return Result.toJson(ScreenSlicer.expandSearchResult(
            Result.instance(reqString)));
      }
    });
  }

  @Path("load-form")
  @POST
  @Consumes("application/json")
  @Produces("application/json; charset=utf-8")
  public static final String loadForm(final String reqString) {
    return process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        return HtmlNode.toJson(ScreenSlicer.loadForm(request, FormLoad.instance(reqString)));
      }
    });
  }

  @Path("extract-person")
  @POST
  @Consumes("application/json")
  @Produces("application/json; charset=utf-8")
  public static final String extractPerson(final String reqString) {
    return process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        return Contact.toJson(ScreenSlicer.extractPerson(request, Extract.instance(reqString)));
      }
    });
  }

  @Path("fetch")
  @POST
  @Consumes("application/json")
  @Produces("text/plain; charset=utf-8")
  public static final String fetch(final String reqString) {
    return process(reqString, new Callback() {
      @Override
      public String perform(Request request) {
        //TODO to be removed in version 2.0.0 after deprecated options removed
        Map<String, Object> map = (Map<String, Object>) CommonUtil.gson.fromJson(reqString, CommonUtil.objectType);
        if (map.containsKey("downloads")) {
          return Result.toJson(ScreenSlicer.fetch(request, Fetch.instance(reqString)));
        }
        return ScreenSlicer._deprecated_fetch(request, Fetch.instance(reqString));
      }
    });
  }

  private static interface Callback {
    String perform(Request request);
  }

  private static String process(String reqString, Callback callback) {
    Request request = Request.instance(reqString);
    String myInstance = null;
    AtomicBoolean myDone = null;
    try {
      Map<String, AtomicBoolean> myDoneMap = new HashMap<String, AtomicBoolean>();
      synchronized (doneMapLock) {
        for (int i = 0; i < request.instances.length; i++) {
          if (!doneMap.containsKey(request.instances[i])) {
            doneMap.put(request.instances[i], new AtomicBoolean(true));
          }
        }
        myDoneMap.putAll(doneMap);
      }
      long myThread = latestThread.incrementAndGet();
      while (true) {
        if (isCancelled(request.runGuid)) {
          curThread.incrementAndGet();
          throw new CancellationException();
        }
        if (myThread == curThread.get() + 1) {
          for (Map.Entry<String, AtomicBoolean> done : myDoneMap.entrySet()) {
            if (done.getValue().compareAndSet(true, false)) {
              if (ScreenSlicer.isBusy(done.getKey())) {
                //TODO fixme?
                done.getValue().set(true);
              } else {
                myInstance = done.getKey();
                myDone = done.getValue();
                break;
              }
            }
          }
          if (myInstance != null) {
            break;
          }
        }
        try {
          Thread.sleep(WAIT);
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
      curThread.incrementAndGet();
      request.instances = new String[] { myInstance };
      return callback.perform(request);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      myDone.set(true);
      synchronized (cancelledLock) {
        cancelledJobs.remove(request.runGuid);
      }
    }
    return null;
  }
}
