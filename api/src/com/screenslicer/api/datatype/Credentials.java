/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.screenslicer.api.datatype;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;

public final class Credentials {
  public static final Credentials instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Credentials> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Credentials instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Credentials.class, args);
  }

  public static final List<Credentials> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Credentials.class, args);
  }

  public static final List<Credentials> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Credentials.class, args);
  }

  public static final String toJson(Credentials obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Credentials>() {}.getType());
  }

  public static final String toJson(Credentials[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Credentials[]>() {}.getType());
  }

  public static final String toJson(List<Credentials> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Credentials>>() {}.getType());
  }

  /**
   * Username for HTML form authentication
   */
  public String username;
  /**
   * Password for HTML form authentication
   */
  public String password;
}
