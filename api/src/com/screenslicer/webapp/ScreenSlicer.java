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
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.screenslicer.webapp.WebApp.Callback;

public final class ScreenSlicer {
  public static interface CustomApp {
    Map<String, Object> configure(Request request, Map<String, Object> args);

    Map<String, List<List<String>>> tableData(Request request, Map<String, Object> args);

    Map<String, byte[]> binaryData(Request request, Map<String, Object> args);

    Map<String, List<Map<String, Object>>> jsonData(Request request, Map<String, Object> args);
  }

  private static SecureRandom rand = new SecureRandom();
  public static final List<Result> NULL_RESULTS = Collections.unmodifiableList(Arrays.asList(new Result[0]));
  public static final List<HtmlNode> NULL_CONTROLS = Collections.unmodifiableList(Arrays.asList(new HtmlNode[0]));
  public static final Contact NULL_CONTACT = new Contact();
  public static final Result NULL_RESULT = new Result();
  public static final String NULL_FETCH = "";

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

  public static final synchronized void startCustomApp(final ScreenSlicer.CustomApp customApp) {
    WebApp.start("custom-app", false, 9000, true, null, new Callback() {
      @Override
      public void call() {
        ScreenSlicerClient.init(customApp);
      }
    });
  }

  public static final boolean isCancelled(String runGuid) {
    return ScreenSlicerDriver.isCancelled(runGuid)
        || ScreenSlicerClient.isCancelled(runGuid);
  }

  public static final void cancel(Cancel args) {
    try {
      for (int i = 0; i < args.instances.length; i++) {
        Request req = new Request();
        req.runGuid = args.runGuid;
        CommonUtil.post("http://" + args.instances[i] + ":8888/core-batch/cancel",
            args.instances[i], Request.toJson(req));
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public static final boolean isBusy(String instanceIp) {
    try {
      return CommonUtil.post("http://" + instanceIp + ":8888/core-batch/busy",
          instanceIp, "") != CommonUtil.NOT_BUSY;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return true;
  }

  public static final void export(Request request, EmailExport args) {
    try {
      if (WebApp.DEV) {
        return;
      }
      CommonUtil.sendEmail(args.recipients, args.title, "Results attached.", args.attachments);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public static final List<Result> queryForm(Request request, FormQuery args) {
    try {
      String instance = instanceIp(request);
      List<Result> ret = CommonUtil.gson.fromJson(
          CommonUtil.post("http://" + instance + ":8888/core-batch/query-form",
              instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<List<Result>>() {}.getType());
      return ret == null ? NULL_RESULTS : ret;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return NULL_RESULTS;
  }

  public static final List<Result> queryKeyword(Request request, KeywordQuery args) {
    try {
      String instance = instanceIp(request);
      List<Result> ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/query-keyword",
          instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<List<Result>>() {}.getType());
      return ret == null ? NULL_RESULTS : ret;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return NULL_RESULTS;
  }

  public static final Result expandSearchResult(Result args) {
    try {
      String instance = args.key.split("@", 2)[0];
      Result ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/expand-search-result",
          instance, Result.toJson(args)),
          new TypeToken<Result>() {}.getType());
      return ret;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return nullSearchResult();
  }

  public static final List<HtmlNode> loadForm(Request request, FormLoad args) {
    try {
      String instance = instanceIp(request);
      List<HtmlNode> ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/load-form",
          instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<List<HtmlNode>>() {}.getType());
      return ret == null ? NULL_CONTROLS : ret;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return NULL_CONTROLS;
  }

  //TODO support multiple people (return a list)
  public static final Contact extractPerson(Request request, Extract args) {
    try {
      String instance = instanceIp(request);
      Contact ret = CommonUtil.gson.fromJson(CommonUtil.post(
          "http://" + instance + ":8888/core-batch/extract-person",
          instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<Contact>() {}.getType());
      return ret == null ? nullContact() : ret;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return nullContact();
  }

  public static final String _deprecated_fetch(Request request, Fetch args) {
    try {
      String instance = instanceIp(request);
      Result result = CommonUtil.gson.fromJson(
          CommonUtil.post("http://" + instance + ":8888/core-batch/fetch",
              instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<Result>() {}.getType());
      return result == null ? NULL_FETCH : (result.pageHtml == null ? NULL_FETCH : result.pageHtml);
    } catch (Throwable t) {
      Log.exception(t);
    }
    return NULL_FETCH;
  }

  public static final Result fetch(Request request, Fetch args) {
    try {
      String instance = instanceIp(request);
      Result result = CommonUtil.gson.fromJson(
          CommonUtil.post("http://" + instance + ":8888/core-batch/fetch",
              instance, CommonUtil.combinedJson(request, args)),
          new TypeToken<Result>() {}.getType());
      return result == null ? NULL_RESULT : result;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return NULL_RESULT;
  }

  private static final String instanceIp(Request request) {
    return request.instances[rand.nextInt(request.instances.length)];
  }
}
