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
package com.screenslicer.api.request;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.api.datatype.Proxy;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Config;
import com.screenslicer.common.Random;

public final class Request {
  static final String[] configInstances = CommonUtil.isEmpty(Config.instance.instances()) ?
      new String[0]
      : (String[]) CommonUtil.gson.fromJson(Config.instance.instances(), CommonUtil.stringArrayType);
  private static final SecureRandom rand = new SecureRandom();
  private static final Proxy[] configProxies = CommonUtil.isEmpty(Config.instance.proxy()) ?
      null : Proxy.instances(Config.instance.proxy()).toArray(new Proxy[0]);

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

  private static final Proxy getConfigProxy() {
    return CommonUtil.isEmpty(configProxies) ? new Proxy() : configProxies[rand.nextInt(configProxies.length)];
  }

  /**
   * IP addresses of ScreenSlicer instances
   */
  public volatile String[] instances = configInstances;
  /**
   * Page load timeout, in seconds.
   * Defaults to 25 seconds.
   */
  public int timeout = 25;
  /**
   * Proxy settings.
   * Defaults to a local tor-socks (socks 5) connection at 9050.
   * This setting is ignored if Request.proxies is not null/empty.
   */
  public Proxy proxy = getConfigProxy();
  /**
   * Proxy settings. Allows multiple proxies. If proxy types overlap, then one
   * is pseudo-randomly chosen. Defaults to null.
   */
  public Proxy[] proxies;
  /**
   * GUID assigned to any ScreenSlicer request.
   * Defaults to a new GUID (just a random string).
   */
  public String runGuid = Random.next();
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
  /**
   * Whether to enable downloads.
   */
  public boolean downloads;
  /**
   * Browser preferences
   * 
   * @deprecated will be removed in version 2.0.0.
   */
  public Map<String, Object> browserPrefs;
  /**
   * HTTP headers added to each request
   */
  public Map<String, String> httpHeaders;
  /**
   * Internal use only.
   */
  public int autoScale = -1;
  /**
   * Internal use only.
   */
  public String[] outputNames;
  /**
   * Internal use only.
   */
  public String appId;
  /**
   * Internal use only.
   */
  public String jobId;
  /**
   * Internal use only.
   */
  public String jobGuid;
}
