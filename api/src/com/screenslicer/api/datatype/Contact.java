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

public final class Contact {
  public static final Contact instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Contact> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Contact instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Contact.class, args);
  }

  public static final List<Contact> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Contact.class, args);
  }

  public static final List<Contact> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Contact.class, args);
  }

  public static final String toJson(Contact obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Contact>() {}.getType());
  }

  public static final String toJson(Contact[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Contact[]>() {}.getType());
  }

  public static final String toJson(List<Contact> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Contact>>() {}.getType());
  }

  /**
   * Name of a person
   */
  public String name;
  /**
   * Email for a person
   */
  public String email;
  /**
   * Phone number for a person
   */
  public String phone;
}
