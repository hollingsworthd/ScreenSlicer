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
package com.screenslicer.api.request;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.api.datatype.Credentials;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.common.CommonUtil;

public final class FormQuery {
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
   * URL of search page
   */
  public String site;
  /**
   * Substrings that URLs of results must contain
   */
  public String[] urlWhitelist;
  /**
   * Regular expressions that URLs of results must match
   */
  public String[] urlPatterns;
  /**
   * Whether to apply the urlWhitelist and urlPatterns before analyzing
   * the page to extract results. This generally produces a more accurate
   * extraction.
   */
  public boolean proactiveUrlFiltering;
  /**
   * Converts result URLs to another format, based on regular expressions.
   */
  public UrlTransform[] urlTransforms;
  /**
   * Maximum number of search pages to extract, unless the
   * results maximum has already been reached.
   * Defaults to 1. Set to 0 or less to disable this maximum.
   */
  public int pages = 1;
  /**
   * Maximum number of results to extract, unless the
   * pages maximum has already been reached.
   * Defaults to 0. Set to 0 or less to disable this maximum.
   */
  public int results = 0;
  /**
   * Whether to get the content at each result URL.
   */
  public boolean fetch;
  /**
   * Whether to visit the result URL directly or try a public web cache.
   */
  public boolean fetchCached;
  /**
   * Whether to extract results or just return all the HTML
   */
  public boolean extract = true;
  /**
   * Override to specify a particular node that's a parent of result nodes to
   * extract.
   */
  public HtmlNode matchParent;
  /**
   * Override to specify a particular node that's a result node to extract.
   */
  public HtmlNode matchResult;
  /**
   * Whether results must have anchors
   */
  public boolean requireResultAnchor = true;
  /**
   * HTML element ID the the search form
   */
  public String formId;
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
  /**
   * Click to submit search.
   */
  public HtmlNode searchSubmitClick;
  /**
   * Clicks on HTML elements prior to authentication.
   */
  public HtmlNode[] preAuthClicks;
  /**
   * Clicks on HTML elements prior to searching.
   */
  public HtmlNode[] preSearchClicks;
  /**
   * Clicks on HTML elements after searching.
   */
  public HtmlNode[] postSearchClicks;
  /**
   * Clicks on HTML elements at a result page after fetching it.
   */
  public HtmlNode[] postFetchClicks;
  /**
   * Credentials for authentication.
   */
  public Credentials credentials;
}
