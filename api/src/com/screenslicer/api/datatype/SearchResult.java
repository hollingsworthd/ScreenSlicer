/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
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

public final class SearchResult {
  public static final SearchResult instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<SearchResult> instances(String json) {
    return instances((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final SearchResult instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(SearchResult.class, args);
  }

  public static final List<SearchResult> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(SearchResult.class, args);
  }

  public static final String toJson(SearchResult obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<SearchResult>() {}.getType());
  }

  public static final String toJson(SearchResult[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<SearchResult[]>() {}.getType());
  }

  public static final String toJson(List<SearchResult> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<SearchResult>>() {}.getType());
  }

  /**
   * Absolute URL of the result
   */
  public String url;
  /**
   * Title of the result as it appears on the search page
   */
  public String title;
  /**
   * Summary of the result as it appears on the search page
   */
  public String summary;
  /**
   * Date of the result as it appears on the search page
   */
  public String date;
  /**
   * HTML fragment associated with the result
   */
  public String html;
  /**
   * HTML of the page at the result URL
   */
  public String pageHtml;
  /**
   * Main portion of text of the page at the result URL
   */
  public String pageText;
}
