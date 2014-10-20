/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
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
package com.screenslicer.api.request;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.api.datatype.Proxy;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Random;

public final class Request {
  public static final Request instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Request> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Request instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Request.class, args);
  }

  public static final List<Request> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Request.class, args);
  }

  public static final List<Request> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Request.class, args);
  }

  public static final String toJson(Request obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Request>() {}.getType());
  }

  public static final String toJson(Request[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Request[]>() {}.getType());
  }

  public static final String toJson(List<Request> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Request>>() {}.getType());
  }

  /**
   * IP addresses of ScreenSlicer instances
   */
  public String[] instances;
  /**
   * Page load timeout, in seconds.
   * Defaults to 25 seconds.
   */
  public int timeout = 25;
  /**
   * Proxy settings.
   * Defaults to a local tor-socks (socks 5) connection at 9050.
   */
  public Proxy proxy = new Proxy();
  /**
   * GUI integration -- specifies a CustomApp ID
   */
  public String appId;
  /**
   * GUI integration -- specifies a configured job name
   */
  public String jobId;
  /**
   * GUI integration -- specifies a configured job GUID
   */
  public String jobGuid;
  /**
   * GUID assigned to any ScreenSlicer request.
   * Defaults to a new GUID (just a random string).
   */
  public String runGuid = Random.next();
  /**
   * GUI integration -- specifies filename(s) of output(s)
   */
  public String[] outputNames;
  /**
   * Whether to email the results of a request
   */
  public boolean emailResults;
  /**
   * Email settings used when emailing the results of a request
   */
  public EmailExport emailExport;
  /**
   * Whether the browser session should be retained from prior request.
   * Defaults to false which is generally what's advisable.
   */
  public boolean continueSession = false;
}
