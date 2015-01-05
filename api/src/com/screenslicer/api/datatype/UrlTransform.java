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
