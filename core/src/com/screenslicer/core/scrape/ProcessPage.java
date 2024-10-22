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
package com.screenslicer.core.scrape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.machinepublishers.browser.Browser;
import com.screenslicer.api.datatype.Result;
import com.screenslicer.api.request.Query;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.scrape.type.ScrapeResult;
import com.screenslicer.core.scrape.type.ScrapeResults;
import com.screenslicer.core.scrape.type.ScrapeResults.Leniency;
import com.screenslicer.core.scrape.type.SearchResults;
import com.screenslicer.core.util.BrowserUtil;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.UrlUtil;
import com.screenslicer.webapp.WebApp;

public class ProcessPage {
  private static final int NUM_EXTRACTIONS = 8;

  private static void trim(Element body) {
    final List<Node> toRemove = new ArrayList<Node>();
    body.traverse(new NodeVisitor() {
      @Override
      public void tail(Node n, int d) {}

      @Override
      public void head(Node node, int d) {
        if (NodeUtil.isHidden(node)) {
          toRemove.add(node);
        }
      }
    });
    for (Node node : toRemove) {
      node.remove();
    }
  }

  public static List<ScrapeResult> perform(Element element, int page, Query query, int thread) {
    try {
      trim(element);
      Map<String, Object> cache = new HashMap<String, Object>();
      return perform(element, page, "", true, query, cache, thread);
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  public static SearchResults perform(Browser browser, int page, Query query, int thread)
      throws ActionFailed {
    try {
      Element element = BrowserUtil.openElement(browser, true,
          query.proactiveUrlFiltering ? query.urlWhitelist : null,
          query.proactiveUrlFiltering ? query.urlPatterns : null,
          query.proactiveUrlFiltering ? query.urlMatchNodes : null,
          query.proactiveUrlFiltering ? query.urlTransforms : null);
      trim(element);
      Map<String, Object> cache = new HashMap<String, Object>();
      List<ScrapeResult> results = perform(
          element, page, browser.getCurrentUrl(), true, query, cache, thread);
      if (results == null || results.isEmpty()) {
        results = perform(element, page, browser.getCurrentUrl(), false, query, cache, thread);
      }
      List<Result> searchResults = new ArrayList<Result>();
      for (ScrapeResult result : results) {
        Result r = new Result();
        r.urlNode = result.urlNode().outerHtml();
        NodeUtil.clean(result.getNodes());
        r.url = result.url();
        r.title = result.title();
        r.date = result.date();
        r.summary = result.summary();
        r.html = NodeUtil.outerHtml(result.getNodes());
        searchResults.add(r);
      }
      return SearchResults.newInstance(true, searchResults, browser.getWindowHandle(), page, query);
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static List<ScrapeResult> perform(Element body, int page, String currentUrl,
      boolean trim, Query query, Map<String, Object> cache, int thread) {
    ScrapeResults ret1 = perform(body, page, Leniency.Title, trim, query, cache, thread);
    if (ret1 != null && !ret1.results().isEmpty()) {
      return finalizeResults(ret1, currentUrl, body, page, Leniency.Title, trim, query, cache, thread);
    }
    ScrapeResults ret2 = perform(body, page, Leniency.None, trim, query, cache, thread);
    if (ret2 != null && !ret2.results().isEmpty()) {
      return finalizeResults(ret2, currentUrl, body, page, Leniency.None, trim, query, cache, thread);
    }
    ScrapeResults ret3 = perform(body, page, Leniency.Url, trim, query, cache, thread);
    if (ret3 != null && !ret3.results().isEmpty()) {
      return finalizeResults(ret3, currentUrl, body, page, Leniency.Url, trim, query, cache, thread);
    }
    return finalizeResults(ret1, currentUrl, body, page, Leniency.Title, trim, query, cache, thread);
  }

  private static List<ScrapeResult> finalizeResults(ScrapeResults results, String currentUrl,
      Element body, int page, Leniency leniency, boolean trim, Query query,
      Map<String, Object> cache, int thread) {
    Log.debug("Returning: (leniency) " + leniency.name(), WebApp.DEBUG);
    if (trim && !results.results().isEmpty()) {
      ScrapeResults untrimmed = perform(body, page, leniency, false, query, cache, thread);
      int trimmedScore = results.fieldScore(true, false);
      int untrimmedScore = untrimmed.fieldScore(true, false);
      if (untrimmedScore > (int) Math.rint(((double) trimmedScore) * 1.05d)) {
        Log.debug("Un-trimmed selected.", WebApp.DEBUG);
        return UrlUtil.fixUrls(untrimmed.results(), currentUrl);
      }
    }
    Log.debug("Trimmed selected.", WebApp.DEBUG);
    return UrlUtil.fixUrls(results.results(), currentUrl);
  }

  private static ScrapeResults perform(Element body, int page,
      Leniency leniency, boolean trim, Query query, Map<String, Object> cache, int thread) {
    Log.debug("-Perform-> " + "leniency=" + leniency.name() + "; trim=" + trim, WebApp.DEBUG);
    Extract.Cache extractCache = cache.containsKey("extractCache")
        ? (Extract.Cache) cache.get("extractCache") : new Extract.Cache();
    cache.put("extractCache", extractCache);
    List<Integer> scores = new ArrayList<Integer>();
    List<ScrapeResults> results = new ArrayList<ScrapeResults>();
    List<Node> ignore = new ArrayList<Node>();
    List<Node> nodes;
    if (!cache.containsKey("extractedNodes")) {
      nodes = new ArrayList<Node>();
      cache.put("extractedNodes", nodes);
      for (int i = 0; i < NUM_EXTRACTIONS;) {
        List<Node> best = Extract.perform(
            body, page, ignore, query.matchResult, query.matchParent, extractCache, thread);
        if (best.isEmpty()) {
          break;
        }
        for (Node node : best) {
          i++;
          nodes.add(node);
          ignore.add(node);
        }
      }
    } else {
      nodes = (List<Node>) cache.get("extractedNodes");
    }
    int pos = 0;
    for (Node node : nodes) {
      ScrapeResults curResults = createResults(body, page, node, pos++, leniency, trim, query, cache);
      results.add(curResults);
      scores.add(curResults.fieldScore(false, trim));
    }
    int max = CommonUtil.max(scores);
    for (int i = 0; i < results.size(); i++) {
      if (results.get(i) != null && scores.get(i) == max) {
        Log.debug("-->results" + (i + 1), WebApp.DEBUG);
        return results.get(i);
      }
    }
    if (!results.isEmpty() && results.get(0) != null) {
      return results.get(0);
    }
    return ScrapeResults.resultsNull;
  }

  private static ScrapeResults createResults(Element body, int page, Node nodeExtract,
      int pos, ScrapeResults.Leniency leniency, boolean trim,
      Query query, Map<String, Object> cache) {
    if (nodeExtract == null) {
      return ScrapeResults.resultsNull;
    }
    try {
      if (!cache.containsKey("createResults")) {
        cache.put("createResults", new HashMap<String, Object>());
      }
      return new ScrapeResults(body, page, nodeExtract, pos, leniency, trim, query,
          (Map<String, Object>) cache.get("createResults"));
    } catch (Throwable t) {
      Log.exception(t);
    }
    return ScrapeResults.resultsNull;
  }

  public static String infoString(List<ScrapeResult> results) {
    int count = 0;
    StringBuilder ret = new StringBuilder();
    if (results == null) {
      ret.append("FAIL");
    } else {
      for (ScrapeResult result : results) {
        ++count;
        ret.append(count
            + " <> " + result.date()
            + " <> " + result.url()
            + " <> " + result.title()
            + " <> " + result.summary() + "\n");
      }
    }
    return ret.toString();
  }
}
