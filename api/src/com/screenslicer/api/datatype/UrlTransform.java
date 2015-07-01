/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * ScreenSlicer is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking ScreenSlicer statically or dynamically with other modules is making a combined work
 *   based on ScreenSlicer. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of ScreenSlicer with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on ScreenSlicer. If you modify ScreenSlicer, you may not
 *   extend this exception to your modified version of ScreenSlicer.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
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
