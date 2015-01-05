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

public final class Fetch {
  public static final Fetch instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Fetch> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Fetch instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Fetch.class, args);
  }

  public static final List<Fetch> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Fetch.class, args);
  }

  public static final List<Fetch> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Fetch.class, args);
  }

  public static final String toJson(Fetch obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Fetch>() {}.getType());
  }

  public static final String toJson(Fetch[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Fetch[]>() {}.getType());
  }

  public static final String toJson(List<Fetch> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Fetch>>() {}.getType());
  }

  /**
   * Whether to visit the result URL directly or try a public web cache
   */
  public boolean fetchCached;
  /**
   * Clicks on HTML elements at a result page after fetching it
   */
  public HtmlNode[] postFetchClicks;
  /**
   * URL to HTTP GET
   */
  public String url;
  /**
   * Whether to throttle requests
   */
  public boolean throttle = true;
}
