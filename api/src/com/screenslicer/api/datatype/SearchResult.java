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

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.common.Random;

public final class SearchResult {
  static {
    File cache = new File("./result_cache/");
    if (!cache.exists()) {
      cache.mkdir();
    }
  }

  public static final SearchResult instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<SearchResult> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final SearchResult instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(SearchResult.class, args);
  }

  public static final List<SearchResult> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(SearchResult.class, args);
  }

  public static final List<SearchResult> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(SearchResult.class, args);
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
   * URL of the result
   */
  public String url;
  /**
   * HTML fragment of the url
   */
  public String urlNode;
  /**
   * Title of the result as it appears on the search page
   */
  public String title;
  /**
   * Summary of the result as it appears on the search page
   */
  public String summary;
  /**
   * Date of the result as it appears on the search page (format: YYYY-MM-DD or
   * YYYY-MM-DD hh:mm:ss)
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
  /**
   * Key for this SearchResult when collapsing is enabled.
   */
  public String key = null;

  public void close() {
    try {
      if (key == null) {
        String id = Random.next();
        key = CommonUtil.myInstance() + "@" + id;
        FileUtils.writeStringToFile(new File("./result_cache/" + id),
            Crypto.encode(CommonUtil.compress(toJson(this))), "utf-8");
        url = null;
        urlNode = null;
        title = null;
        summary = null;
        date = null;
        html = null;
        pageHtml = null;
        pageText = null;
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public void remove() {
    if (key != null) {
      String[] parts = key.split("@", 2);
      FileUtils.deleteQuietly(new File("./result_cache/" + parts[1]));
    }
  }

  public void open() {
    try {
      if (key != null) {
        String[] parts = key.split("@", 2);
        File file = new File("./result_cache/" + parts[1]);
        SearchResult cached = instance(CommonUtil.decompress(Crypto.decode(
            FileUtils.readFileToString(file, "utf-8"))));
        FileUtils.deleteQuietly(file);
        this.url = cached.url;
        this.urlNode = cached.urlNode;
        this.title = cached.title;
        this.summary = cached.summary;
        this.date = cached.date;
        this.html = cached.html;
        this.pageHtml = cached.pageHtml;
        this.pageText = cached.pageText;
        key = null;
        cached = null;
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }
}
