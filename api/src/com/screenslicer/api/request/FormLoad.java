/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License version 3
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * version 3 along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
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
