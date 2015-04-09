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

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.reflect.TypeToken;
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

/**
 * @Deprecated This class will be renamed in version 2.0.0
 */
public final class ScreenSlicer {
  public static interface Listener {
    void starting(Request request, Object auxiliaryObj);

    void finished(Request request, Object auxiliaryObj);

    void busy(Request request, Object auxiliaryObj);
  }

  private static SecureRandom rand = new SecureRandom();
  private static final Collection<String> busyInstances = new HashSet<String>();
  private static final AtomicLong latestThread = new AtomicLong();
  private static final AtomicLong curThread = new AtomicLong();
  private static final long WAIT = 10000;
  public static final List<Result> NULL_RESULTS = Collections.unmodifiableList(Arrays.asList(new Result[0]));
  public static final List<HtmlNode> NULL_CONTROLS = Collections.unmodifiableList(Arrays.asList(new HtmlNode[0]));
  public static final Contact NULL_CONTACT = new Contact();
  public static final Result NULL_RESULT = new Result();
  public static final String NULL_FETCH = "";
  private static final AtomicReference<Listener> listener = new AtomicReference<Listener>(new Listener() {
    @Override
    public void starting(Request request, Object auxiliaryObj) {}

    @Override
    public void finished(Request request, Object auxiliaryObj) {}

    @Override
    public void busy(Request request, Object auxiliaryObj) {}
  });

  public static void setListener(Listener newListener) {
    listener.set(newListener);
  }

  private static final Contact nullContact() {
    for (Field field : Contact.class.getFields()) {
      try {
        field.set(NULL_CONTACT, null);
      } catch (Throwable t) {}
    }
    return NULL_CONTACT;
  }

  private static final Result nullSearchResult() {
    for (Field field : Result.class.getFields()) {
      try {
        field.set(NULL_RESULT, null);
      } catch (Throwable t) {}
    }
    return NULL_RESULT;
  }

  public static final boolean isCancelled(String runGuid) {
    return ScreenSlicerDriver.isCancelled(runGuid);
  }

  public static final void cancel(Cancel args) {
    Request req = new Request();
    req.runGuid = args.runGuid;
    req.instances = args.instances;
    listener.get().starting(req, args);
    try {
      for (int i = 0; i < req.instances.length; i++) {
        CommonUtil.post("http://" + req.instances[i] + ":8888/core-batch/cancel",
            req.instances[i], Request.toJson(req));
      }
    } catch (Throwable t) {
      Log.exception(t);

    } finally {
      listener.get().finished(req, args);
    }
  }

  public static final boolean isBusy(String instanceIp) {
    try {
      synchronized (busyInstances) {
        if (busyInstances.contains(instanceIp)) {
          return true;
        }
      }
      return CommonUtil.post("http://" + instanceIp + ":8888/core-batch/busy",
          instanceIp, "") != CommonUtil.NOT_BUSY;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return true;
  }

  public static final void export(Request request, EmailExport args) {
    listener.get().starting(request, args);
    try {
      if (WebApp.DEV) {
        return;
      }
      CommonUtil.sendEmail(args.recipients, args.title, "Results attached.", args.attachments);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      listener.get().finished(request, args);
    }
  }

  public static final List<Result> queryForm(Request request, FormQuery args) {
    listener.get().starting(request, args);
    String instance = instanceIp(request, args);
    try {
      List<Result> ret = CommonUtil.gson.fromJson(
          CommonUtil.post("http://" + instance + ":8888/core-batch/query-form",
              instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<List<Result>>() {}.getType());
      return ret == null ? NULL_RESULTS : ret;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (busyInstances) {
        busyInstances.remove(instance);
      }
      listener.get().finished(request, args);
    }
    return NULL_RESULTS;
  }

  public static final List<Result> queryKeyword(Request request, KeywordQuery args) {
    listener.get().starting(request, args);
    String instance = instanceIp(request, args);
    try {
      List<Result> ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/query-keyword",
          instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<List<Result>>() {}.getType());
      return ret == null ? NULL_RESULTS : ret;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (busyInstances) {
        busyInstances.remove(instance);
      }
      listener.get().finished(request, args);
    }
    return NULL_RESULTS;
  }

  public static final Result expandSearchResult(Result args) {
    Request request = new Request();
    listener.get().starting(request, args);
    try {
      String instance = args.key.split("@", 2)[0];
      Result ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/expand-search-result",
          instance, Result.toJson(args)),
          new TypeToken<Result>() {}.getType());
      return ret;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      listener.get().finished(request, args);
    }
    return nullSearchResult();
  }

  public static final List<HtmlNode> loadForm(Request request, FormLoad args) {
    listener.get().starting(request, args);
    String instance = instanceIp(request, args);
    try {
      List<HtmlNode> ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/load-form",
          instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<List<HtmlNode>>() {}.getType());
      return ret == null ? NULL_CONTROLS : ret;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (busyInstances) {
        busyInstances.remove(instance);
      }
      listener.get().finished(request, args);
    }
    return NULL_CONTROLS;
  }

  //TODO support multiple people (return a list)
  public static final Contact extractPerson(Request request, Extract args) {
    listener.get().starting(request, args);
    String instance = instanceIp(request, args);
    try {
      Contact ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/extract-person",
          instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<Contact>() {}.getType());
      return ret == null ? nullContact() : ret;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (busyInstances) {
        busyInstances.remove(instance);
      }
      listener.get().finished(request, args);
    }
    return nullContact();
  }

  public static final String _deprecated_fetch(Request request, Fetch args) {
    listener.get().starting(request, args);
    String instance = instanceIp(request, args);
    try {
      Result result = CommonUtil.gson.fromJson(
          CommonUtil.post("http://" + instance + ":8888/core-batch/fetch",
              instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<Result>() {}.getType());
      return result == null ? NULL_FETCH : (result.pageHtml == null ? NULL_FETCH : result.pageHtml);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (busyInstances) {
        busyInstances.remove(instance);
      }
      listener.get().finished(request, args);
    }
    return NULL_FETCH;
  }

  public static final Result fetch(Request request, Fetch args) {
    listener.get().starting(request, args);
    String instance = instanceIp(request, args);
    try {
      Result result = CommonUtil.gson.fromJson(
          CommonUtil.post("http://" + instance + ":8888/core-batch/fetch",
              instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<Result>() {}.getType());
      return result == null ? NULL_RESULT : result;

    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (busyInstances) {
        busyInstances.remove(instance);
      }
      listener.get().finished(request, args);
    }
    return NULL_RESULT;
  }

  private static final String instanceIp(Request request, Object args) {
    try {
      final long myThread = latestThread.incrementAndGet();
      String myInstance = null;
      while (true) {
        if (myThread == curThread.get() + 1) {
          synchronized (busyInstances) {
            for (int i = 0; i < request.instances.length; i++) {
              if (!ScreenSlicer.isBusy(request.instances[i])) {
                myInstance = request.instances[i];
                busyInstances.add(myInstance);
              }
            }
          }
        }
        if (myInstance == null) {
          listener.get().busy(request, args);
          try {
            Thread.sleep(WAIT);
          } catch (InterruptedException e) {}
        } else {
          break;
        }
      }
      return myInstance;
    } catch (Throwable t) {
      Log.exception(t);
      return request.instances[rand.nextInt(request.instances.length)];
    } finally {
      curThread.incrementAndGet();
    }
  }
}
