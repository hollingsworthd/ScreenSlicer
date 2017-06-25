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
import com.screenslicer.api.datatype.Credentials;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.common.CommonUtil;

public final class FormLoad {
  public static final FormLoad instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<FormLoad> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final FormLoad instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(FormLoad.class, args);
  }

  public static final List<FormLoad> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(FormLoad.class, args);
  }

  public static final List<FormLoad> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(FormLoad.class, args);
  }

  public static final String toJson(FormLoad obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<FormLoad>() {}.getType());
  }

  public static final String toJson(FormLoad[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<FormLoad[]>() {}.getType());
  }

  public static final String toJson(List<FormLoad> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<FormLoad>>() {}.getType());
  }

  /**
   * URL of search page
   */
  public String site;
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
   * Clicks on HTML elements prior to authentication.
   */
  public HtmlNode[] preAuthClicks;
  /**
   * Clicks on HTML elements prior to searching.
   */
  public HtmlNode[] preSearchClicks;
  /**
   * Credentials for authentication.
   */
  public Credentials credentials;
}
