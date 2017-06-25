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

public final class FormQuery extends Query {
  public static final FormQuery instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<FormQuery> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final FormQuery instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(FormQuery.class, args);
  }

  public static final List<FormQuery> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(FormQuery.class, args);
  }

  public static final List<FormQuery> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(FormQuery.class, args);
  }

  public static final String toJson(FormQuery obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<FormQuery>() {}.getType());
  }

  public static final String toJson(FormQuery[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<FormQuery[]>() {}.getType());
  }

  public static final String toJson(List<FormQuery> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<FormQuery>>() {}.getType());
  }

  /**
   * HTML element ID the the search form
   * 
   * @deprecated will be removed in version 2.0.0.
   */
  public String formId;
  /**
   * The target html form.
   */
  public HtmlNode form;
  /**
   * Schema of the search form (obtained from load-form function)
   */
  public HtmlNode[] formSchema;
  /**
   * Model of the search form.
   * The map keys are GUIDs from the formSchema, and the map values
   * are lists of strings. Except for multi-select inputs (i.e., selects or
   * checkboxes) the list should only have one string.
   */
  public Map<String, List<String>> formModel;

  @Override
  public boolean isKeywordQuery() {
    return false;
  }

  @Override
  public boolean isFormQuery() {
    return true;
  }
}
