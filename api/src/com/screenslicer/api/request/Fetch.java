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

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.common.CommonUtil;

public final class Fetch {
  public static final Fetch instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Fetch> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Fetch instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Fetch.class, args);
  }

  public static final List<Fetch> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Fetch.class, args);
  }

  public static final List<Fetch> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Fetch.class, args);
  }

  public static final String toJson(Fetch obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Fetch>() {}.getType());
  }

  public static final String toJson(Fetch[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Fetch[]>() {}.getType());
  }

  public static final String toJson(List<Fetch> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Fetch>>() {}.getType());
  }

  /**
   * Whether to visit the result URL directly or try a public web cache
   */
  public boolean fetchCached;
  /**
   * Clicks on HTML elements at a result page after fetching it
   */
  public HtmlNode[] postFetchClicks;
  /**
   * Attach media to the result which match these HtmlNodes
   */
  public HtmlNode[] media;
  /**
   * URL to HTTP GET
   */
  public String url;
  /**
   * Whether to throttle requests
   */
  public boolean throttle = true;
}
