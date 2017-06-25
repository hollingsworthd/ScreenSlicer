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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.common.Random;

public final class Result {
  static {
    File cache = new File("./result_cache/");
    if (!cache.exists()) {
      cache.mkdir();
    }
  }

  public static final Result instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Result> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Result instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Result.class, args);
  }

  public static final List<Result> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Result.class, args);
  }

  public static final List<Result> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Result.class, args);
  }

  public static final String toJson(Result obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Result>() {}.getType());
  }

  public static final String toJson(Result[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Result[]>() {}.getType());
  }

  public static final String toJson(List<Result> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Result>>() {}.getType());
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
   * Media binary content. The map key is the "src" attribute of the HTML element
   * associated with media content. The map value is a base64-encoded string of the binary content.
   */
  public final Map<String, String> mediaBinaries = new LinkedHashMap<String, String>();
  /**
   * Media mime types. The map key is the "src" attribute of the HTML element
   * associated with media content. The map value is a the mime type of the binary content.
   */
  public final Map<String, String> mediaMimeTypes = new LinkedHashMap<String, String>();
  /**
   * HTML of the page at the result URL
   */
  public String pageHtml;
  /**
   * Main portion of text of the page at the result URL
   */
  public String pageText;
  /**
   * Binary content of the page. A base-64 encoded String representing a byte
   * array.
   */
  //TODO make this an array to support multiple attachments
  public String pageBinary;
  /**
   * Mime-type of the binary content;
   */
  public String pageBinaryMimeType;
  /**
   * Filename extension of the binary content;
   */
  public String pageBinaryExtension;
  /**
   * Filename of the binary content;
   */
  public String pageBinaryFilename;
  /**
   * Key for this SearchResult when collapsing is enabled. Do not edit/update this value.
   */
  public String key = null;

  private static final AtomicLong holdCount = new AtomicLong();

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
        mediaBinaries.clear();
        mediaMimeTypes.clear();
        pageHtml = null;
        pageText = null;
        pageBinary = null;
        pageBinaryMimeType = null;
        pageBinaryExtension = null;
        pageBinaryFilename = null;
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public boolean isClosed() {
    return key != null;
  }

  public void remove() {
    try {
      if (key != null) {
        String[] parts = key.split("@", 2);
        FileUtils.deleteQuietly(new File("./result_cache/" + parts[1]));
      }
    } finally {
      key = null;
    }
  }

  public static boolean hasHolds() {
    return holdCount.get() != 0;
  }

  public static void removeHold() {
    holdCount.decrementAndGet();
  }

  public static void addHold() {
    holdCount.incrementAndGet();
  }

  public boolean open() {
    boolean success = false;
    try {
      if (key != null) {
        success = true;
        String[] parts = key.split("@", 2);
        File file = new File("./result_cache/" + parts[1]);
        Result cached = instance(CommonUtil.decompress(Crypto.decode(
            FileUtils.readFileToString(file, "utf-8"))));
        FileUtils.deleteQuietly(file);
        this.url = cached.url;
        this.urlNode = cached.urlNode;
        this.title = cached.title;
        this.summary = cached.summary;
        this.date = cached.date;
        this.html = cached.html;
        this.mediaBinaries.clear();
        this.mediaBinaries.putAll(cached.mediaBinaries);
        this.mediaMimeTypes.clear();
        this.mediaMimeTypes.putAll(cached.mediaMimeTypes);
        this.pageHtml = cached.pageHtml;
        this.pageText = cached.pageText;
        this.pageBinary = cached.pageBinary;
        this.pageBinaryMimeType = cached.pageBinaryMimeType;
        this.pageBinaryExtension = cached.pageBinaryExtension;
        this.pageBinaryFilename = cached.pageBinaryFilename;
        cached = null;
      }
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      key = null;
    }
    return success;
  }
}
