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
package com.screenslicer.core.scrape;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.Node;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.Proxy;
import com.screenslicer.api.datatype.SearchResult;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.api.request.Fetch;
import com.screenslicer.api.request.FormLoad;
import com.screenslicer.api.request.FormQuery;
import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.api.request.Query;
import com.screenslicer.api.request.Request;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.common.Random;
import com.screenslicer.core.scrape.Proceed.End;
import com.screenslicer.core.scrape.neural.NeuralNetManager;
import com.screenslicer.core.scrape.type.Result;
import com.screenslicer.core.service.ScreenSlicerBatch;
import com.screenslicer.core.util.Util;
import com.screenslicer.webapp.WebApp;

import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;

public class Scrape {
  static {
    try {
      FileUtils.deleteQuietly(new File("./fetch_local_cache"));
      FileUtils.forceMkdir(new File("./fetch_local_cache"));
    } catch (Throwable t) {
      Log.exception(t);
    }
  }
  private static volatile RemoteWebDriver driver = null;
  private static final int HANG_TIME = 10 * 60 * 1000;
  private static final int RETRIES = 7;
  private static final long WAIT = 2000;
  private static AtomicLong latestThread = new AtomicLong();
  private static AtomicLong curThread = new AtomicLong();
  private static final Object cacheLock = new Object();
  private static final Map<String, List> nextResults = new HashMap<String, List>();
  private static List<String> cacheKeys = new ArrayList<String>();
  private static final int LIMIT_CACHE = 5000;
  private static final int MAX_CACHE = 500;
  private static final int CLEAR_CACHE = 250;
  private static AtomicBoolean done = new AtomicBoolean(false);
  private static final Object progressLock = new Object();
  private static String progress1Key = "";
  private static String progress2Key = "";
  private static String progress1 = "";
  private static String progress2 = "";
  private static final Object fetchLocalCacheLock = new Object();
  private static final Map<String, Long> fetchLocalCache = new HashMap<String, Long>();
  private static final int MAX_FETCH_LOCAL_CACHE = 1000;
  private static final int MIN_FETCH_CACHE_PAGE_LEN = 2500;
  private static final long FETCH_LOCAL_CACHE_EXPIRES = 2 * 60 * 60 * 1000;

  public static final List<Result> WAITING = new ArrayList<Result>();

  public static class ActionFailed extends Exception {
    private static final long serialVersionUID = 1L;

    public ActionFailed() {
      super();
    }

    public ActionFailed(Throwable nested) {
      super(nested);
    }

    public ActionFailed(String message) {
      super(message);
    }
  }

  public static void init() {
    NeuralNetManager.reset(new File("./resources/neural/config"));
    start(new Request());
    done.set(true);
  }

  private static void start(Request req) {
    Proxy[] proxies = CommonUtil.isEmpty(req.proxies) ? new Proxy[] { req.proxy } : req.proxies;
    for (int i = 0; i < RETRIES; i++) {
      try {
        FirefoxProfile profile = new FirefoxProfile(new File("./firefox-profile"));
        for (int curProxy = 0; curProxy < proxies.length; curProxy++) {
          Proxy proxy = proxies[curProxy];
          if (proxy != null) {
            if (!CommonUtil.isEmpty(proxy.username) || !CommonUtil.isEmpty(proxy.password)) {
              String user = proxy.username == null ? "" : proxy.username;
              String pass = proxy.password == null ? "" : proxy.password;
              profile.setPreference("extensions.closeproxyauth.authtoken",
                  Base64.encodeBase64String((user + ":" + pass).getBytes("utf-8")));
            } else {
              profile.setPreference("extensions.closeproxyauth.authtoken", "");
            }
            if (Proxy.TYPE_ALL.equals(proxy.type)
                || Proxy.TYPE_SOCKS_5.equals(proxy.type)
                || Proxy.TYPE_SOCKS_4.equals(proxy.type)) {
              profile.setPreference("network.proxy.type", 1);
              profile.setPreference("network.proxy.socks", proxy.ip);
              profile.setPreference("network.proxy.socks_port", proxy.port);
              profile.setPreference("network.proxy.socks_remote_dns", true);
              profile.setPreference("network.proxy.socks_version",
                  Proxy.TYPE_ALL.equals(proxy.type) || Proxy.TYPE_SOCKS_5.equals(proxy.type) ? 5 : 4);
            }
            if (Proxy.TYPE_ALL.equals(proxy.type)
                || Proxy.TYPE_SSL.equals(proxy.type)) {
              profile.setPreference("network.proxy.type", 1);
              profile.setPreference("network.proxy.ssl", proxy.ip);
              profile.setPreference("network.proxy.ssl_port", proxy.port);
            }
            if (Proxy.TYPE_ALL.equals(proxy.type)
                || Proxy.TYPE_HTTP.equals(proxy.type)) {
              profile.setPreference("network.proxy.type", 1);
              profile.setPreference("network.proxy.http", proxy.ip);
              profile.setPreference("network.proxy.http_port", proxy.port);
            }
          }
        }
        if (req.browserPrefs != null) {
          for (Map.Entry<String, Object> entry : req.browserPrefs.entrySet()) {
            if (entry.getValue() instanceof Integer) {
              profile.setPreference(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
              profile.setPreference(entry.getKey(), (int) Math.rint(((Double) entry.getValue())));
            } else if (entry.getValue() instanceof Boolean) {
              profile.setPreference(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() instanceof String) {
              profile.setPreference(entry.getKey(), (String) entry.getValue());
            }
          }
        }
        driver = new FirefoxDriver(profile);
        driver.manage().timeouts().pageLoadTimeout(req.timeout, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(req.timeout, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        break;
      } catch (Throwable t1) {
        if (driver != null) {
          try {
            forceQuit();
            driver = null;
          } catch (Throwable t2) {
            Log.exception(t2);
          }
        }
        Log.exception(t1);
      }
    }
  }

  public static void forceQuit() {
    try {
      if (driver != null) {
        ((FirefoxDriver) driver).kill();
        Util.driverSleepStartup();
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    try {
      TemporaryFilesystem tempFS = TemporaryFilesystem.getDefaultTmpFS();
      tempFS.deleteTemporaryFiles();
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void restart(Request req) {
    try {
      forceQuit();
    } catch (Throwable t) {
      Log.exception(t);
    }
    try {
      driver = null;
    } catch (Throwable t) {
      Log.exception(t);
    }
    start(req);
  }

  private static void push(String mapKey, List results) {
    synchronized (cacheLock) {
      nextResults.put(mapKey, results);
      if (nextResults.size() == LIMIT_CACHE) {
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, List> entry : nextResults.entrySet()) {
          if (!cacheKeys.contains(entry.getKey())
              && !entry.getKey().equals(mapKey)) {
            toRemove.add(entry.getKey());
          }
        }
        for (String key : toRemove) {
          nextResults.remove(key);
        }
        nextResults.put(mapKey, results);
      }
      if (results != null && !results.isEmpty()) {
        if (cacheKeys.size() == MAX_CACHE) {
          List<String> newCache = new ArrayList<String>();
          for (int i = 0; i < CLEAR_CACHE; i++) {
            nextResults.remove(cacheKeys.get(i));
          }
          for (int i = CLEAR_CACHE; i < MAX_CACHE; i++) {
            newCache.add(cacheKeys.get(i));
          }
          cacheKeys = newCache;
        }
        cacheKeys.add(mapKey);
      }
    }
  }

  public static List<Result> cached(String mapKey) {
    synchronized (cacheLock) {
      if (nextResults.containsKey(mapKey)) {
        List<Result> ret = nextResults.get(mapKey);
        if (ret == null) {
          return WAITING;
        }
        return ret;
      } else {
        return null;
      }
    }
  }

  public static boolean busy() {
    return !done.get();
  }

  public static String progress(String mapKey) {
    synchronized (progressLock) {
      if (progress1Key.equals(mapKey)) {
        return progress1;
      }
      if (progress2Key.equals(mapKey)) {
        return progress2;
      }
      return "";
    }
  }

  private static String toCacheUrl(String url, boolean fallback) {
    if (url == null) {
      return null;
    }
    if (fallback) {
      return "http://webcache.googleusercontent.com/search?q=cache:" + url.split("://")[1];
    }
    String[] urlParts = url.split("://")[1].split("/", 2);
    String urlLhs = urlParts[0];
    String urlRhs = urlParts.length > 1 ? urlParts[1] : "";
    return "http://" + urlLhs + ".nyud.net:8080/" + urlRhs;
  }

  private static void fetch(RemoteWebDriver driver, Request req, Query query, Query recQuery,
      List<Result> results, boolean cleanupWindows, List<SearchResult> recResults) throws ActionFailed {
    try {
      String origHandle = driver.getWindowHandle();
      String origUrl = driver.getCurrentUrl();
      String newHandle = null;
      if (query.fetchCached) {
        newHandle = Util.newWindow(driver, cleanupWindows);
      }
      try {
        for (Result result : results) {
          if (query.requireResultAnchor && !isUrlValid(result.url())
              && Util.uriScheme.matcher(result.url()).matches()) {
            continue;
          }
          if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
            return;
          }
          Log.info("Fetching URL " + result.url() + ". Cached: " + query.fetchCached, false);
          try {
            result.attach(getHelper(driver, query.throttle, result.urlNode(), result.url(), query.fetchCached,
                req.runGuid, cleanupWindows && query == null, query == null ? null : query.postFetchClicks));
            if (recQuery != null) {
              req.continueSession = true;
              recResults.addAll(scrape(recQuery, req, true));
            }

          } catch (Throwable t) {
            Log.exception(t);
          }
          try {
            if (!driver.getWindowHandle().equals(origHandle)) {
              driver.close();
              driver.switchTo().window(origHandle);
              driver.switchTo().defaultContent();
            }
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      } catch (Throwable t) {
        Log.exception(t);
      } finally {
        if (query.fetchCached && origHandle.equals(newHandle)) {
          Log.exception(new Throwable("Failed opening new window"));
          Util.get(driver, origUrl, true, cleanupWindows);
        } else {
          Util.handleNewWindows(driver, origHandle, cleanupWindows);
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    } finally {
      Util.driverSleepRand(query.throttle);
    }
  }

  private static String getHelper(final RemoteWebDriver driver, final boolean throttle,
      final Node urlNode, final String url, final boolean p_cached, final String runGuid,
      final boolean cleanupWindows, final HtmlNode[] postFetchClicks) {
    final String urlHash = CommonUtil.isEmpty(url) ? null : Crypto.fastHash(url);
    final long time = System.currentTimeMillis();
    if (urlHash != null) {
      synchronized (fetchLocalCacheLock) {
        if (fetchLocalCache.containsKey(urlHash)) {
          if (time - fetchLocalCache.get(urlHash) < FETCH_LOCAL_CACHE_EXPIRES) {
            try {
              return FileUtils.readFileToString(new File("./fetch_local_cache/" + urlHash), "utf-8");
            } catch (Throwable t) {
              Log.exception(t);
              fetchLocalCache.remove(urlHash);
            }
          } else {
            fetchLocalCache.remove(urlHash);
          }
        }
      }
    }
    if (!CommonUtil.isEmpty(url) || urlNode != null) {
      final Object resultLock = new Object();
      final String initVal;
      final String[] result;
      synchronized (resultLock) {
        initVal = Random.next();
        result = new String[] { initVal };
      }
      final AtomicBoolean started = new AtomicBoolean();
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          started.set(true);
          boolean cached = p_cached;
          String newHandle = null;
          String origHandle = null;
          try {
            origHandle = driver.getWindowHandle();
            String content = null;
            if (!cached) {
              try {
                Util.get(driver, url, urlNode, false, cleanupWindows);
              } catch (Throwable t) {
                if (urlNode != null) {
                  Util.newWindow(driver, cleanupWindows);
                }
                Util.get(driver, url, false, cleanupWindows);
              }
              if (urlNode != null) {
                newHandle = driver.getWindowHandle();
              }
              Util.doClicks(driver, postFetchClicks, null);
              content = driver.getPageSource();
              if (CommonUtil.isEmpty(content)) {
                cached = true;
              }
            }
            if (cached) {
              if (ScreenSlicerBatch.isCancelled(runGuid)) {
                return;
              }
              try {
                Util.get(driver, toCacheUrl(url, false), false, cleanupWindows);
              } catch (Throwable t) {
                Util.get(driver, toCacheUrl(url, true), false, cleanupWindows);
              }
              content = driver.getPageSource();
            }
            content = Util.clean(content, driver.getCurrentUrl()).outerHtml();
            if (WebApp.DEBUG) {
              try {
                FileUtils.writeStringToFile(new File("./" + System.currentTimeMillis() + ".log.fetch"), content, "utf-8");
              } catch (IOException e) {}
            }
            //TODO make iframes work
            //            if (!CommonUtil.isEmpty(content)) {
            //              Document doc = Jsoup.parse(content);
            //              Elements docFrames = doc.getElementsByTag("iframe");
            //              List<WebElement> iframes = driver.findElementsByTagName("iframe");
            //              int cur = 0;
            //              for (WebElement iframe : iframes) {
            //                try {
            //                  driver.switchTo().frame(iframe);
            //                  String frameSrc = driver.getPageSource();
            //                  if (!CommonUtil.isEmpty(frameSrc) && cur < docFrames.size()) {
            //                    docFrames.get(cur).html(
            //                        Util.outerHtml(Jsoup.parse(frameSrc).body().childNodes()));
            //                  }
            //                } catch (Throwable t) {
            //                  Log.exception(t);
            //                }
            //                ++cur;
            //              }
            //              driver.switchTo().defaultContent();
            //              content = doc.outerHtml();
            //            }
            synchronized (resultLock) {
              result[0] = content;
            }
          } catch (Throwable t) {
            Log.exception(t);
          } finally {
            synchronized (resultLock) {
              if (initVal.equals(result[0])) {
                result[0] = null;
              }
            }
            Util.driverSleepRand(throttle);
            if (cleanupWindows && newHandle != null && origHandle != null) {
              try {
                Util.handleNewWindows(driver, origHandle, true);
              } catch (Throwable t) {
                Log.exception(t);
              }
            }
          }
        }
      });
      thread.start();
      try {
        while (!started.get()) {
          try {
            Thread.sleep(WAIT);
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
        thread.join(HANG_TIME);
        synchronized (resultLock) {
          if (initVal.equals(result[0])) {
            try {
              Log.exception(new Exception("Browser is hanging"));
              forceQuit();
              thread.interrupt();
            } catch (Throwable t) {
              Log.exception(t);
            }
            throw new ActionFailed();
          } else if (urlHash != null && !CommonUtil.isEmpty(result[0])
              && result[0].length() > MIN_FETCH_CACHE_PAGE_LEN) {
            synchronized (fetchLocalCacheLock) {
              if (fetchLocalCache.size() > MAX_FETCH_LOCAL_CACHE) {
                try {
                  FileUtils.deleteQuietly(new File("./fetch_local_cache"));
                  FileUtils.forceMkdir(new File("./fetch_local_cache"));
                } catch (Throwable t) {
                  Log.exception(t);
                }
                fetchLocalCache.clear();
              }
              FileUtils.writeStringToFile(new File("./fetch_local_cache/" + urlHash), result[0], "utf-8", false);
              fetchLocalCache.put(urlHash, time);
            }
          }
          return result[0];
        }
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return null;
  }

  public static String get(Fetch fetch, Request req) {
    if (!isUrlValid(fetch.url)) {
      return null;
    }
    long myThread = latestThread.incrementAndGet();
    while (myThread != curThread.get() + 1
        || !done.compareAndSet(true, false)) {
      try {
        Thread.sleep(WAIT);
      } catch (Exception e) {
        Log.exception(e);
      }
    }
    if (!req.continueSession) {
      restart(req);
    }
    Log.info("Get URL " + fetch.url + ". Cached: " + fetch.fetchCached, false);
    String resp = "";
    try {
      resp = getHelper(driver, fetch.throttle, null, fetch.url, fetch.fetchCached, req.runGuid, true, fetch.postFetchClicks);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      curThread.incrementAndGet();
      done.set(true);
    }
    return resp;
  }

  private static List<Result> filterResults(List<Result> results, String[] whitelist,
      String[] patterns, UrlTransform[] urlTransforms, boolean forExport) {
    List<Result> filtered = new ArrayList<Result>();
    if (results == null) {
      return filtered;
    }
    results = Util.transformUrls(results, urlTransforms, forExport);
    if ((whitelist == null || whitelist.length == 0)
        && (patterns == null || patterns.length == 0)) {
      filtered = results;
    } else {
      for (Result result : results) {
        if (!Util.isResultFiltered(result, whitelist, patterns)) {
          filtered.add(result);
        }
      }
      if (filtered.isEmpty() && !results.isEmpty()) {
        Log.warn("Filtered every url, e.g., " + results.get(0).url());
      }
    }
    return filtered;
  }

  public static List<HtmlNode> loadForm(FormLoad context, Request req) throws ActionFailed {
    if (!isUrlValid(context.site)) {
      return new ArrayList<HtmlNode>();
    }
    long myThread = latestThread.incrementAndGet();
    while (myThread != curThread.get() + 1
        || !done.compareAndSet(true, false)) {
      try {
        Thread.sleep(WAIT);
      } catch (Exception e) {
        Log.exception(e);
      }
    }
    if (!req.continueSession) {
      restart(req);
    }
    try {
      List<HtmlNode> ret = null;
      try {
        ret = QueryForm.load(driver, context, true);
      } catch (Throwable t) {
        if (!req.continueSession) {
          restart(req);
        }
        ret = QueryForm.load(driver, context, true);
      }
      return ret;
    } finally {
      curThread.incrementAndGet();
      done.set(true);
    }
  }

  private static void handlePage(Request req, Query query, int page, boolean recursive,
      List<Result> results, List<SearchResult> recResults, List<String> resultPages) throws ActionFailed {
    if (query.extract) {
      List<Result> newResults = ProcessPage.perform(driver, page, query);
      newResults = filterResults(newResults, query.urlWhitelist, query.urlPatterns, query.urlTransforms, false);
      if (query.results > 0 && results.size() + newResults.size() > query.results) {
        int remove = results.size() + newResults.size() - query.results;
        for (int i = 0; i < remove && !newResults.isEmpty(); i++) {
          newResults.remove(newResults.size() - 1);
        }
      }
      if (query.fetch) {
        fetch(driver, req, query,
            query.keywordQuery == null ? (query.formQuery == null ? null : query.formQuery) : query.keywordQuery,
            newResults, !recursive, recResults);
        results.addAll(newResults);
      }
    } else {
      resultPages.add(Util.clean(driver.getPageSource(), driver.getCurrentUrl()).outerHtml());
    }
  }

  public static List<SearchResult> scrape(Query query, Request req) {
    return scrape(query, req, false);
  }

  private static List<SearchResult> scrape(Query query, Request req, boolean recursive) {
    if (!recursive) {
      long myThread = latestThread.incrementAndGet();
      while (myThread != curThread.get() + 1
          || !done.compareAndSet(true, false)) {
        try {
          Thread.sleep(WAIT);
        } catch (Exception e) {
          Log.exception(e);
        }
      }
    }
    if (!req.continueSession) {
      restart(req);
    }
    CommonUtil.clearStripCache();
    Util.clearOuterHtmlCache();
    List<Result> results = new ArrayList<Result>();
    List<SearchResult> recResults = new ArrayList<SearchResult>();
    List<String> resultPages = new ArrayList<String>();
    try {
      if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
        throw new Exception("Cancellation was requested");
      }
      if (query.isFormQuery()) {
        Log.info("FormQuery for URL " + query.site, false);
        try {
          QueryForm.perform(driver, (FormQuery) query, !recursive);
        } catch (Throwable e) {
          restart(req);
          QueryForm.perform(driver, (FormQuery) query, !recursive);
        }
      } else {
        Log.info("KewordQuery for URL " + query.site + ". Query: " + ((KeywordQuery) query).keywords, false);
        try {
          QueryKeyword.perform(driver, (KeywordQuery) query, !recursive);
        } catch (Throwable e) {
          restart(req);
          QueryKeyword.perform(driver, (KeywordQuery) query, !recursive);
        }
      }
      if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
        throw new Exception("Cancellation was requested");
      }
      handlePage(req, query, 1, recursive, results, recResults, resultPages);
      String priorProceedLabel = null;
      for (int page = 2; (page <= query.pages || query.pages <= 0)
          && (results.size() < query.results || query.results <= 0); page++) {
        if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
          throw new Exception("Cancellation was requested");
        }
        if (!query.fetch) {
          try {
            Util.driverSleepRand(query.throttle);
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
        priorProceedLabel = Proceed.perform(driver, page, priorProceedLabel);
        if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
          throw new Exception("Cancellation was requested");
        }
        handlePage(req, query, page, recursive, results, recResults, resultPages);
      }
    } catch (End e) {
      Log.info("Reached end of results", false);
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      if (!recursive) {
        curThread.incrementAndGet();
        done.set(true);
      }
    }
    if (query.extract) {
      if (recResults.isEmpty()) {
        List<SearchResult> extractedResults = new ArrayList<SearchResult>();
        if (results != null) {
          results = filterResults(results, query.urlWhitelist, query.urlPatterns, query.urlTransforms, true);
          for (Result result : results) {
            Util.clean(result.getNodes());
            SearchResult r = new SearchResult();
            r.url = result.url();
            r.title = result.title();
            r.date = result.date();
            r.summary = result.summary();
            r.html = Util.outerHtml(result.getNodes());
            r.pageHtml = result.attachment();
            if (!CommonUtil.isEmpty(r.pageHtml)) {
              try {
                r.pageText = NumWordsRulesExtractor.INSTANCE.getText(r.pageHtml);
              } catch (Throwable t) {
                r.pageText = null;
                Log.exception(t);
              }
            }
            extractedResults.add(r);
          }
        }
        return extractedResults;
      }
      return recResults;
    }
    List<SearchResult> pages = new ArrayList<SearchResult>();
    for (String page : resultPages) {
      SearchResult r = new SearchResult();
      r.html = page;
      pages.add(r);
    }
    return pages;
  }

  private static boolean isUrlValid(String url) {
    return !CommonUtil.isEmpty(url) && (url.startsWith("https://") || url.startsWith("http://"));
  }

  public static List<Result> scrape(String url, final String query, final int pages, final String mapKey1, final String mapKey2) {
    if (!isUrlValid(url)) {
      return new ArrayList<Result>();
    }
    if (!done.compareAndSet(true, false)) {
      return null;
    }
    restart(new Request());
    CommonUtil.clearStripCache();
    Util.clearOuterHtmlCache();
    List<Result> results = new ArrayList<Result>();
    final KeywordQuery keywordQuery = new KeywordQuery();
    try {
      synchronized (progressLock) {
        progress1Key = mapKey1;
        progress2Key = mapKey2;
        progress1 = "Page 1 progress: performing search query...";
        progress2 = "Page 2 progress: waiting for prior page extraction to finish...";
      }
      push(mapKey1, null);
      keywordQuery.site = url;
      keywordQuery.keywords = query;
      QueryKeyword.perform(driver, keywordQuery, true);
      synchronized (progressLock) {
        progress1 = "Page 1 progress: extracting results...";
      }
      results.addAll(ProcessPage.perform(driver, 1, keywordQuery));
      synchronized (progressLock) {
        progress1 = "";
      }
    } catch (Throwable t) {
      Log.exception(t);
      push(mapKey1, results);
      synchronized (progressLock) {
        progress1 = "";
        progress2 = "Page 2 progress: prior page extraction was not completed.";
      }
      done.set(true);
      return results;
    }
    try {
      push(mapKey2, null);
      push(mapKey1, results);
    } catch (Throwable t) {
      Log.exception(t);
      synchronized (progressLock) {
        progress1 = "";
        progress2 = "Page 2 progress: prior page extraction was not completed.";
      }
      done.set(true);
      return results;
    }
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<Result> next = new ArrayList<Result>();
        try {
          synchronized (progressLock) {
            progress2 = "Page 2 progress: getting page...";
          }
          Proceed.perform(driver, 2, query);
          synchronized (progressLock) {
            progress2 = "Page 2 progress: extracting results...";
          }
          next.addAll(ProcessPage.perform(driver, 2, keywordQuery));
        } catch (End e) {
          Log.info("Reached end of results", false);
        } catch (Throwable t) {
          Log.exception(t);
        }
        finally {
          push(mapKey2, next);
          synchronized (progressLock) {
            progress2 = "";
          }
          done.set(true);
        }
      }
    }).start();
    return results;
  }
}
