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
package com.screenslicer.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Node;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.type.ScrapeResult;
import com.screenslicer.core.scrape.type.SearchResults;
import com.screenslicer.webapp.WebApp;

public class UrlUtil {
  public static final Pattern uriScheme = Pattern.compile("^[A-Za-z0-9]*:.*$");
  private static final String schemeFragment = "^[A-Za-z0-9]*:?(?://)?";

  static boolean isUrlFiltered(String currentUrl, String url, Node urlNode,
      String[] whitelist, String[] patterns, HtmlNode[] urlNodes, UrlTransform[] transforms) {
    url = transformUrl(url, transforms, false);
    if (!CommonUtil.isEmpty(url)) {
      if (!CommonUtil.isEmpty(currentUrl)) {
        url = toCleanUrl(currentUrl, url);
      }
      int max = CommonUtil.max(whitelist == null ? 0 : whitelist.length,
          patterns == null ? 0 : patterns.length,
          urlNodes == null ? 0 : urlNodes.length);
      for (int i = 0; i < max; i++) {
        if (!CommonUtil.isEmpty(url)) {
          if (whitelist != null && i < whitelist.length
              && url.toLowerCase().contains(whitelist[i].toLowerCase())) {
            return false;
          }
          if (patterns != null && i < patterns.length
              && url.toLowerCase().matches(patterns[i].toLowerCase())) {
            return false;
          }
        }
        if (urlNodes != null && i < urlNodes.length
            && NodeUtil.matches(urlNodes[i], urlNode)) {
          return false;
        }
      }
    }
    return true;
  }

  public static List<ScrapeResult> fixUrls(List<ScrapeResult> results, String currentUrl) {
    if (results == null) {
      return null;
    }
    List<String> urls = new ArrayList<String>();
    for (ScrapeResult result : results) {
      urls.add(result.url());
    }
    urls = fixUrlStrings(urls, currentUrl);
    for (int i = 0; i < results.size(); i++) {
      results.get(i).tweakUrl(urls.get(i));
    }
    return results;
  }

  private static String transformUrl(String url, UrlTransform[] urlTransforms, boolean forExport) {
    List<String> urls = new ArrayList<String>();
    urls.add(url);
    return transformUrlStrings(urls, urlTransforms, forExport).get(0);
  }

  public static SearchResults transformUrls(SearchResults results, UrlTransform[] urlTransforms, boolean forExport) {
    if (results == null) {
      return null;
    }
    List<String> urls = new ArrayList<String>();
    for (int i = 0; i < results.size(); i++) {
      urls.add(results.get(i).url);
    }
    urls = transformUrlStrings(urls, urlTransforms, forExport);
    for (int i = 0; urls != null && i < results.size(); i++) {
      results.get(i).url = urls.get(i);
    }
    return results;
  }

  private static List<String> transformUrlStrings(List<String> urls, UrlTransform[] urlTransforms, boolean forExport) {
    List<String> newUrls = new ArrayList<String>();
    if (urlTransforms != null && urlTransforms.length != 0 && urls != null) {
      for (String url : urls) {
        String newUrl = url;
        for (int i = 0; urlTransforms != null && i < urlTransforms.length; i++) {
          if (!CommonUtil.isEmpty(urlTransforms[i].regex)
              && newUrl != null
              && urlTransforms[i] != null
              && ((forExport && urlTransforms[i].transformForExportOnly)
              || (!forExport && !urlTransforms[i].transformForExportOnly))) {
            Pattern pattern = Pattern.compile(urlTransforms[i].regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(newUrl);
            if (matcher.find()) {
              if (urlTransforms[i].replaceAll) {
                if (urlTransforms[i].replaceAllRecursive) {
                  String transformed = matcher.replaceAll(urlTransforms[i].replacement);
                  String transformedRec = pattern.matcher(transformed).replaceAll(urlTransforms[i].replacement);
                  while (!transformed.equals(transformedRec)) {
                    transformed = transformedRec;
                    transformedRec = pattern.matcher(transformedRec).replaceAll(urlTransforms[i].replacement);
                  }
                  newUrl = transformed;
                } else {
                  newUrl = matcher.replaceAll(urlTransforms[i].replacement);
                }
              } else {
                newUrl = matcher.replaceFirst(urlTransforms[i].replacement);
              }
              if (!urlTransforms[i].multipleTransforms) {
                break;
              }
            }
          }
        }
        newUrls.add(newUrl);
      }
    } else {
      return urls;
    }
    return newUrls;
  }

  private static List<String> fixUrlStrings(List<String> urls, String currentUrl) {
    if (urls == null) {
      return null;
    }
    if (currentUrl == null) {
      return urls;
    }
    String absolute = currentUrl;
    int slash = absolute.lastIndexOf('/');
    if (slash == -1) {
      return urls;
    }
    int query = absolute.indexOf('?');
    if (query > -1) {
      absolute = absolute.substring(0, query);
    }
    int hash = absolute.indexOf('#');
    if (hash > -1) {
      absolute = absolute.substring(0, hash);
    }
    int amp = absolute.indexOf('&');
    if (amp > -1) {
      absolute = absolute.substring(0, amp);
    }
    int schemeIndex = absolute.contains("://") ? absolute.indexOf("://") + 3 : 0;
    int relativeIndex = absolute.lastIndexOf('/');
    relativeIndex = relativeIndex == -1 ? absolute.length() : relativeIndex;
    String relative = absolute.substring(0, relativeIndex) + "/";
    int absIndex = absolute.indexOf('/', schemeIndex);
    absolute = absIndex > -1 ? absolute.substring(0, absolute.indexOf('/', schemeIndex)) + "/" : absolute + "/";
    List<String> newUrls = new ArrayList<String>();
    for (String url : urls) {
      String newUrl;
      if (CommonUtil.isEmpty(url)) {
        newUrl = url;
      } else if (url.startsWith("//")) {
        newUrl = (currentUrl.startsWith("https:") ? "https:" : "http:") + url;
      } else if (url.startsWith("http://localhost/")) {
        newUrl = absolute +
            url.substring("http://localhost/".length() - 1, url.length());
      } else if (url.startsWith("/")) {
        newUrl = absolute + url.substring(1);
      } else if (!url.contains("://")) {
        newUrl = relative + url;
      } else {
        newUrl = url;
      }
      newUrls.add(newUrl);
    }
    if (WebApp.DEBUG) {
      for (String url : urls) {
        Log.debug("result: " + url, WebApp.DEBUG);
      }
    }
    return newUrls;
  }

  private static String toCleanUrl(String fullUrl, String href) {
    String clean = toCanonicalUri(fullUrl, href);
    return clean.startsWith("//") ? (fullUrl.startsWith("https:") ? "https:" + clean : "http:" + clean) : clean;
  }

  static String toCanonicalUri(String fullUrl, String href) {
    if (href.startsWith("/") && !href.startsWith("//")) {
      return getUriBase(fullUrl, false) + href;
    }
    if (!uriScheme.matcher(href).matches() && !href.startsWith("/")) {
      return getUriBase(fullUrl, true) + href;
    }
    if (!uriScheme.matcher(href).matches() && !href.startsWith("//")) {
      return "//" + href.replaceAll(schemeFragment, "");
    }

    return href;
  }

  private static String getUriBase(String fullUri, boolean relative) {
    if (fullUri.contains("?")) {
      fullUri = fullUri.split("\\?")[0];
    }
    if (fullUri.contains("#")) {
      fullUri = fullUri.split("#")[0];
    }
    if (fullUri.contains("&")) {
      fullUri = fullUri.split("&")[0];
    }
    if (relative) {
      String relativeBase = "";
      int index = fullUri.lastIndexOf('/');
      int doubleSlashIndex = fullUri.indexOf("//");
      if (index > doubleSlashIndex + 1) {
        relativeBase = fullUri.substring(0, index + 1);
      } else {
        relativeBase = fullUri;
      }
      relativeBase = relativeBase.replaceAll(schemeFragment, "");
      relativeBase = "//" + relativeBase;
      if (!relativeBase.endsWith("/")) {
        relativeBase = relativeBase + "/";
      }
      return relativeBase;
    }
    String absoluteBase = "";
    int start = 0;
    int doubleSlash = fullUri.indexOf("//");
    if (doubleSlash > -1) {
      start = doubleSlash + "//".length();
    }
    int index = fullUri.indexOf('/', start);
    if (index > -1) {
      absoluteBase = fullUri.substring(0, index);
    } else {
      absoluteBase = fullUri;
    }
    absoluteBase = absoluteBase.replaceAll(schemeFragment, "");
    absoluteBase = "//" + absoluteBase;
    return absoluteBase;
  }

  public static String urlFromAttr(Node node) {
    for (Attribute attr : node.attributes().asList()) {
      if (attr.getValue().contains("://")) {
        return attr.getValue();
      }
    }
    return null;
  }
}
