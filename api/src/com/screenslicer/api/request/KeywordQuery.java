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

public final class KeywordQuery extends Query {
  public static final KeywordQuery instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<KeywordQuery> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final KeywordQuery instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(KeywordQuery.class, args);
  }

  public static final List<KeywordQuery> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(KeywordQuery.class, args);
  }

  public static final List<KeywordQuery> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(KeywordQuery.class, args);
  }

  public static final String toJson(KeywordQuery obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<KeywordQuery>() {}.getType());
  }

  public static final String toJson(KeywordQuery[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<KeywordQuery[]>() {}.getType());
  }

  public static final String toJson(List<KeywordQuery> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<KeywordQuery>>() {}.getType());
  }

  /**
   * Text to enter into search box
   */
  public String keywords;

  @Override
  public boolean isKeywordQuery() {
    return true;
  }

  @Override
  public boolean isFormQuery() {
    return false;
  }
}
