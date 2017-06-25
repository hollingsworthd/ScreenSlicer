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
import com.screenslicer.common.CommonUtil;

public final class Cancel {
  public static final Cancel instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Cancel> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Cancel instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Cancel.class, args);
  }

  public static final List<Cancel> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Cancel.class, args);
  }

  public static final List<Cancel> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Cancel.class, args);
  }

  public static final String toJson(Cancel obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Cancel>() {}.getType());
  }

  public static final String toJson(Cancel[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Cancel[]>() {}.getType());
  }

  public static final String toJson(List<Cancel> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Cancel>>() {}.getType());
  }

  /**
   * IP addresses of ScreenSlicer instances
   */
  public volatile String[] instances = Request.configInstances;
  /**
   * GUID of the run to cancel
   */
  public String runGuid;
}
