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

public final class Extract {
  public static final Extract instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Extract> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Extract instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Extract.class, args);
  }

  public static final List<Extract> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Extract.class, args);
  }

  public static final List<Extract> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Extract.class, args);
  }

  public static final String toJson(Extract obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Extract>() {}.getType());
  }

  public static final String toJson(Extract[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Extract[]>() {}.getType());
  }

  public static final String toJson(List<Extract> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Extract>>() {}.getType());
  }

  /**
   * Content to extract entities from
   */
  public String content;
}
