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
package com.screenslicer.api.datatype;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;

public final class UrlTransform {
  public static final UrlTransform instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<UrlTransform> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final UrlTransform instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(UrlTransform.class, args);
  }

  public static final List<UrlTransform> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(UrlTransform.class, args);
  }

  public static final List<UrlTransform> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(UrlTransform.class, args);
  }

  public static final String toJson(UrlTransform obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<UrlTransform>() {}.getType());
  }

  public static final String toJson(UrlTransform[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<UrlTransform[]>() {}.getType());
  }

  public static final String toJson(List<UrlTransform> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<UrlTransform>>() {}.getType());
  }

  /**
   * Regular expression to match against a URL
   */
  public String regex;
  /**
   * Replacement for the regex match
   */
  public String replacement;
  /**
   * Whether to replace all occurrences of the regex (i.e., a global flag for
   * the regex)
   */
  public boolean replaceAll = false;
  /**
   * Whether to run the transform again on a previously transformed URL
   */
  public boolean replaceAllRecursive = false;
  /**
   * When multiple transforms are specified, whether to keep applying them after
   * the first has already been matched
   */
  public boolean multipleTransforms = false;
  /**
   * Whether to only apply the transform immediately before returning the
   * results. Otherwise, this transform will affect any fetch operations.
   */
  public boolean transformForExportOnly = false;
}
