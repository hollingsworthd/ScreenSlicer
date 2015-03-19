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
