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
package com.screenslicer.core.scrape;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.openqa.selenium.OutputType;

import com.google.common.io.Files;
import com.machinepublishers.browser.Browser;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.ProxyConfig;
import com.machinepublishers.jbrowserdriver.ProxyConfig.Type;
import com.machinepublishers.jbrowserdriver.RequestHeaders;
import com.machinepublishers.jbrowserdriver.Settings;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.Proxy;
import com.screenslicer.api.datatype.Result;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.api.request.Fetch;
import com.screenslicer.api.request.FormLoad;
import com.screenslicer.api.request.FormQuery;
import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.api.request.Query;
import com.screenslicer.api.request.Request;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.common.Random;
import com.screenslicer.core.scrape.Proceed.End;
import com.screenslicer.core.scrape.neural.NeuralNetManager;
import com.screenslicer.core.scrape.type.SearchResults;
import com.screenslicer.core.service.ScreenSlicerBatch;
import com.screenslicer.core.util.BrowserUtil;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.UrlUtil;
import com.screenslicer.webapp.WebApp;

import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;

public class Scrape {
  public static class ActionFailed extends Exception {
    private static final long serialVersionUID = 1L;

    public ActionFailed() {
      super();
    }

    public ActionFailed(Throwable nested) {
      super(nested);
      Log.exception(nested);
    }

    public ActionFailed(String message) {
      super(message);
    }
  }

  public static class Cancelled extends Exception {

  }

  private static final AtomicReference<Browser[]> browsers =
      new AtomicReference<Browser[]>(new Browser[WebApp.THREADS]);
  private static final int MAX_INIT = 1000;
  private static final int HANG_TIME = 10 * 60 * 1000;
  private static final long WAIT = 2000;
  private static final Object cacheLock = new Object();
  private static final AtomicReference<Map<String, List>> nextResults =
      new AtomicReference<Map<String, List>>(new HashMap<String, List>());
  private static final AtomicReference<List<String>> cacheKeys =
      new AtomicReference<List<String>>(new ArrayList<String>());
  private static final int LIMIT_CACHE = 5000;
  private static final int MAX_CACHE = 500;
  private static final int CLEAR_CACHE = 250;
  private static final boolean[] done = new boolean[WebApp.THREADS];
  private static final Object doneLock = new Object();
  private static final Object progressLock = new Object();
  private static String progress1Key = "";
  private static String progress2Key = "";
  private static String progress1 = "";
  private static String progress2 = "";

  public static final List<Result> WAITING = new ArrayList<Result>();

  public static void init() {
    for (int i = 0; i < WebApp.THREADS; i++) {
      NeuralNetManager.reset(new File("./resources/neural/config"), i);
      start(new Request(), false, i);
      synchronized (doneLock) {
        done[i] = true;
      }
    }
  }

  private static void start(Request req, boolean media, int threadNum) {
    Type proxyType = null;
    String proxyHost = null;
    int proxyPort = -1;
    String proxyUser = null;
    String proxyPassword = null;
    Proxy[] proxies = CommonUtil.isEmpty(req.proxies) ? new Proxy[] { req.proxy } : req.proxies;
    for (int curProxy = 0; curProxy < proxies.length; curProxy++) {
      Proxy proxy = proxies[curProxy];
      if (proxy != null) {
        proxyType = proxy.type.equals(Proxy.TYPE_SOCKS)
            || proxy.type.equals(Proxy.TYPE_ALL)
            || proxy.type.equals(Proxy.TYPE_SOCKS_4)
            || proxy.type.equals(Proxy.TYPE_SOCKS_5)
            ? Type.SOCKS : (proxy.type.equals(Proxy.TYPE_HTTP) || proxy.type.equals(Proxy.TYPE_SSL)
                ? Type.HTTP : null);
        proxyHost = proxy.ip;
        proxyPort = proxy.port;
        if (!CommonUtil.isEmpty(proxy.username) || !CommonUtil.isEmpty(proxy.password)) {
          proxyUser = proxy.username == null ? "" : proxy.username;
          proxyPassword = proxy.password == null ? "" : proxy.password;
        }
      }
    }
    File downloadCache = new File("./download_cache" + threadNum);
    FileUtils.deleteQuietly(downloadCache);
    downloadCache.mkdir();
    File mediaCache = null;
    if (media) {
      mediaCache = new File("./media_cache" + threadNum);
      FileUtils.deleteQuietly(mediaCache);
      mediaCache.mkdir();
    }
    browsers.get()[threadNum] = new JBrowserDriver(
        new Settings.Builder().
            requestHeaders(req.httpHeaders == null ? null
                : new RequestHeaders(new LinkedHashMap<String, String>(req.httpHeaders))).
            proxy(new ProxyConfig(proxyType, proxyHost, proxyPort, proxyUser, proxyPassword)).
            downloadDir(downloadCache).
            mediaDir(mediaCache).build());
    browsers.get()[threadNum].manage().timeouts().pageLoadTimeout(req.timeout, TimeUnit.SECONDS);
    browsers.get()[threadNum].manage().timeouts().setScriptTimeout(req.timeout, TimeUnit.SECONDS);
    browsers.get()[threadNum].manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
  }

  public static void forceQuit(int threadNum) {
    try {
      if (browsers.get()[threadNum] != null) {
        browsers.get()[threadNum].kill();
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void restart(Request req, boolean media, int threadNum) {
    try {
      forceQuit(threadNum);
    } catch (Throwable t) {
      Log.exception(t);
    }
    try {
      browsers.get()[threadNum] = null;
    } catch (Throwable t) {
      Log.exception(t);
    }
    start(req, media, threadNum);
  }

  private static void push(String mapKey, List results) {
    synchronized (cacheLock) {
      nextResults.get().put(mapKey, results);
      if (nextResults.get().size() == LIMIT_CACHE) {
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, List> entry : nextResults.get().entrySet()) {
          if (!cacheKeys.get().contains(entry.getKey())
              && !entry.getKey().equals(mapKey)) {
            toRemove.add(entry.getKey());
          }
        }
        for (String key : toRemove) {
          nextResults.get().remove(key);
        }
        nextResults.get().put(mapKey, results);
      }
      if (results != null && !results.isEmpty()) {
        if (cacheKeys.get().size() == MAX_CACHE) {
          List<String> newCache = new ArrayList<String>();
          for (int i = 0; i < CLEAR_CACHE; i++) {
            nextResults.get().remove(cacheKeys.get().get(i));
          }
          for (int i = CLEAR_CACHE; i < MAX_CACHE; i++) {
            newCache.add(cacheKeys.get().get(i));
          }
          cacheKeys.set(newCache);
        }
        cacheKeys.get().add(mapKey);
      }
    }
  }

  public static List<Result> cached(String mapKey) {
    synchronized (cacheLock) {
      if (nextResults.get().containsKey(mapKey)) {
        List<Result> ret = nextResults.get().get(mapKey);
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
    synchronized (doneLock) {
      for (int i = 0; i < WebApp.THREADS; i++) {
        if (done[i]) {
          return false;
        }
      }
    }
    return true;
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

  private static class DownloadedFiles {
    String content;
    String mimeType;
    String extension;
    String filename;

    public DownloadedFiles(int thread) {
      File file = new File("./download_cache" + thread);
      Collection<File> list = FileUtils.listFiles(file, null, false);
      if (!list.isEmpty()) {
        try {
          File download = list.iterator().next();
          byte[] bytes = FileUtils.readFileToByteArray(download);
          content = Base64.encodeBase64String(bytes);
          filename = download.getName();
          mimeType = new Tika().detect(bytes, filename);
          int index = filename.lastIndexOf(".");
          if (index > -1 && index < filename.length()) {
            extension = filename.substring(index + 1).toLowerCase();
            filename = filename.substring(0, index);
          }
        } catch (Throwable t) {
          Log.exception(t);
        } finally {
          for (File cur : list) {
            FileUtils.deleteQuietly(cur);
          }
        }
      }
    }
  }

  private static class SavedMedia {
    Map<String, String> encodedBytes = new LinkedHashMap<String, String>();
    Map<String, String> mimeTypes = new LinkedHashMap<String, String>();

    public SavedMedia(String body, HtmlNode[] patterns, boolean allMedia, int thread) {
      if (allMedia || !CommonUtil.isEmpty(patterns)) {
        Document doc = CommonUtil.parse(body, null, false);
        List<Element> elementsTmp = new ArrayList<Element>(doc.getElementsByAttribute("src"));
        List<Element> elements = new ArrayList<Element>();
        if (!CommonUtil.isEmpty(patterns)) {
          for (Element element : elementsTmp) {
            for (int i = 0; i < patterns.length; i++) {
              if (NodeUtil.matches(patterns[i], element)) {
                elements.add(element);
                break;
              }
            }
          }
        }
        if (allMedia || !elements.isEmpty()) {
          try {
            File dir = new File("./media_cache" + thread);
            Collection<File> list = FileUtils.listFiles(dir, new String[] { "content" }, false);
            File savedMeta = null;
            for (File savedContent : list) {
              try {
                byte[] rawContent = FileUtils.readFileToByteArray(savedContent);
                String content = Base64.encodeBase64String(rawContent);
                savedMeta = new File(dir, savedContent.getName().split("\\.")[0] + ".metadata");
                List<String> lines =
                    FileUtils.readLines(savedMeta);
                String url = lines.get(0);
                String reportedMimeType = lines.size() >= 2 ? lines.get(1) : "";
                String detectedMimeType = new Tika().detect(rawContent);
                String mimeType = !CommonUtil.isEmpty(reportedMimeType)
                    && !reportedMimeType.toLowerCase().contains("octet") ? reportedMimeType
                    : (!CommonUtil.isEmpty(detectedMimeType) ? detectedMimeType : reportedMimeType);
                List<String> sources = sources(url, elements);
                if (sources.isEmpty() && allMedia) {
                  encodedBytes.put(url, content);
                  mimeTypes.put(url, mimeType);
                } else {
                  for (String src : sources) {
                    encodedBytes.put(src, content);
                    mimeTypes.put(src, mimeType);
                  }
                }
              } catch (Throwable t) {
                Log.exception(t);
              } finally {
                FileUtils.deleteQuietly(savedContent);
                FileUtils.deleteQuietly(savedMeta);
              }
            }
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      }
    }

    private static List<String> sources(String url, List<Element> elements) {
      List<String> sources = new ArrayList<String>();
      for (Element element : elements) {
        String src = element.attr("src");
        if (!CommonUtil.isEmpty(src) && url.endsWith(src)) {
          sources.add(src);
        }
      }
      return sources;
    }
  }

  private static void fetch(Browser browser, Context context) throws ActionFailed {
    boolean terminate = false;
    try {
      String origHandle = browser.getWindowHandle();
      String origUrl = browser.getCurrentUrl();
      String newHandle = null;
      if (context.query.fetchCached) {
        newHandle = BrowserUtil.newWindow(browser, context.depth == 0);
      }
      try {
        for (int i = context.query.currentResult(); i < context.newResults.size(); i++) {
          if (context.query.requireResultAnchor && !isUrlValid(context.newResults.get(i).url)
              && UrlUtil.uriScheme.matcher(context.newResults.get(i).url).matches()) {
            context.newResults.get(i).close();
            context.query.markResult(i + 1);
            continue;
          }
          if (ScreenSlicerBatch.isCancelled(context.req.runGuid)) {
            return;
          }
          Log.info("Fetching URL " + context.newResults.get(i).url
              + ". Cached: " + context.query.fetchCached, false);
          try {
            context.newResults.get(i).pageHtml = getHelper(browser, context.query.throttle,
                CommonUtil.parseFragment(context.newResults.get(i).urlNode, false),
                context.newResults.get(i).url, context.query.fetchCached,
                context.req.runGuid, context.query.fetchInNewWindow,
                context.depth == 0 && context.query == null,
                context.query == null ? null : context.query.postFetchClicks);
            //TODO get downloads and media for the results page, not just fetched pages
            DownloadedFiles downloaded = new DownloadedFiles(context.threadNum);
            context.newResults.get(i).pageBinary = downloaded.content;
            context.newResults.get(i).pageBinaryMimeType = downloaded.mimeType;
            context.newResults.get(i).pageBinaryExtension = downloaded.extension;
            context.newResults.get(i).pageBinaryFilename = downloaded.filename;
            SavedMedia media = new SavedMedia(context.newResults.get(i).pageHtml,
                context.query.media, context.query.allMedia, context.threadNum);
            context.newResults.get(i).mediaBinaries.putAll(media.encodedBytes);
            context.newResults.get(i).mediaMimeTypes.putAll(media.mimeTypes);
            if (!CommonUtil.isEmpty(context.newResults.get(i).pageHtml)) {
              try {
                context.newResults.get(i).pageText =
                    NumWordsRulesExtractor.INSTANCE.getText(context.newResults.get(i).pageHtml);
              } catch (Throwable t) {
                context.newResults.get(i).pageText = null;
                Log.exception(t);
              }
            }
            if (context.recQuery != null) {
              context.recResults.addPage(scrape(context.recQuery, context.req, context.depth + 1, false,
                  context.media, context.cache, context.threadNum));
            }
            if (context.query.collapse) {
              context.newResults.get(i).close();
            }
            context.query.markResult(i + 1);
          } catch (Browser.Retry r) {
            terminate = true;
            throw r;
          } catch (Browser.Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            terminate = true;
            throw new ActionFailed(t);
          }
          try {
            if (!browser.getWindowHandle().equals(origHandle)) {
              browser.close();
              browser.switchTo().window(origHandle);
              browser.switchTo().defaultContent();
            } else if (!context.query.fetchInNewWindow) {
              BrowserUtil.get(browser, origUrl, true, context.depth == 0);
              SearchResults.revalidate(browser, false, context.threadNum);
            }
          } catch (Browser.Retry r) {
            terminate = true;
            throw r;
          } catch (Browser.Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            terminate = true;
            throw new ActionFailed(t);
          }
        }
      } catch (Browser.Retry r) {
        terminate = true;
        throw r;
      } catch (Browser.Fatal f) {
        terminate = true;
        throw f;
      } catch (Throwable t) {
        terminate = true;
        throw new ActionFailed(t);
      } finally {
        if (!terminate) {
          if (!context.query.fetchInNewWindow
              || (context.query.fetchCached && origHandle.equals(newHandle))) {
            if (context.query.fetchInNewWindow) {
              Log.exception(new Throwable("Failed opening new window"));
            }
            BrowserUtil.get(browser, origUrl, true, context.depth == 0);
          } else {
            BrowserUtil.handleNewWindows(browser, origHandle, context.depth == 0);
          }
        }
      }
    } catch (Browser.Retry r) {
      terminate = true;
      throw r;
    } catch (Browser.Fatal f) {
      terminate = true;
      throw f;
    } catch (Throwable t) {
      terminate = true;
      throw new ActionFailed(t);
    } finally {
      if (!terminate) {
        BrowserUtil.browserSleepLong(context.query.throttle);
      }
    }
  }

  private static String getHelper(final Browser browser, final boolean throttle,
      final Node urlNode, final String url, final boolean p_cached, final String runGuid,
      final boolean toNewWindow, final boolean init, final HtmlNode[] postFetchClicks) {
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
          boolean terminate = false;
          started.set(true);
          boolean cached = p_cached;
          String newHandle = null;
          String origHandle = null;
          try {
            origHandle = browser.getWindowHandle();
            String content = null;
            if (!cached) {
              try {
                BrowserUtil.get(browser, url, urlNode, false, toNewWindow, init);
              } catch (Browser.Retry r) {
                terminate = true;
                throw r;
              } catch (Browser.Fatal f) {
                terminate = true;
                throw f;
              } catch (Throwable t) {
                if (urlNode != null) {
                  BrowserUtil.newWindow(browser, init);
                }
                BrowserUtil.get(browser, url, false, init);
              }
              if (urlNode != null) {
                newHandle = browser.getWindowHandle();
              }
              BrowserUtil.doClicks(browser, postFetchClicks, null, null);
              content = browser.getPageSource();
              if (WebApp.DEBUG && (postFetchClicks == null || postFetchClicks.length == 0)) {
                try {
                  long filename = System.currentTimeMillis();
                  Files.copy(browser.getScreenshotAs(OutputType.FILE),
                      new File("./" + filename + ".log.scrape.png"));
                  FileUtils.writeStringToFile(
                      new File("./" + filename + ".log.scrape.htm"),
                      content, "utf-8");
                } catch (IOException e) {}
              }
              if (CommonUtil.isEmpty(content)) {
                cached = true;
              }
            }
            if (cached) {
              if (ScreenSlicerBatch.isCancelled(runGuid)) {
                return;
              }
              try {
                BrowserUtil.get(browser, toCacheUrl(url, false), false, init);
              } catch (Browser.Retry r) {
                terminate = true;
                throw r;
              } catch (Browser.Fatal f) {
                terminate = true;
                throw f;
              } catch (Throwable t) {
                BrowserUtil.get(browser, toCacheUrl(url, true), false, init);
              }
              content = browser.getPageSource();
            }
            content = NodeUtil.clean(content, browser.getCurrentUrl()).outerHtml();
            //TODO make iframes work
            //            if (!CommonUtil.isEmpty(content)) {
            //              Document doc = Jsoup.parse(content);
            //              Elements docFrames = doc.getElementsByTag("iframe");
            //              List<WebElement> iframes = browser.findElementsByTagName("iframe");
            //              int cur = 0;
            //              for (WebElement iframe : iframes) {
            //                try {
            //                  browser.switchTo().frame(iframe);
            //                  String frameSrc = browser.getPageSource();
            //                  if (!CommonUtil.isEmpty(frameSrc) && cur < docFrames.size()) {
            //                    docFrames.get(cur).html(
            //                        Util.outerHtml(Jsoup.parse(frameSrc).body().childNodes()));
            //                  }
            //                } catch (Throwable t) {
            //                  Log.exception(t);
            //                }
            //                ++cur;
            //              }
            //              browser.switchTo().defaultContent();
            //              content = doc.outerHtml();
            //            }
            synchronized (resultLock) {
              result[0] = content;
            }
          } catch (Browser.Retry r) {
            terminate = true;
            throw r;
          } catch (Browser.Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
          } finally {
            synchronized (resultLock) {
              if (initVal.equals(result[0])) {
                result[0] = null;
              }
            }
            if (!terminate) {
              BrowserUtil.browserSleepLong(throttle);
              if (init && newHandle != null && origHandle != null) {
                try {
                  BrowserUtil.handleNewWindows(browser, origHandle, true);
                } catch (Browser.Retry r) {
                  throw r;
                } catch (Browser.Fatal f) {
                  throw f;
                } catch (Throwable t) {
                  Log.exception(t);
                }
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
          } catch (Throwable t) {}
        }
        thread.join(HANG_TIME);
        synchronized (resultLock) {
          if (initVal.equals(result[0])) {
            Log.exception(new Exception("Browser is hanging"));
            try {
              thread.interrupt();
            } catch (Throwable t) {
              Log.exception(t);
            }
            throw new Browser.Retry();
          }
          return result[0];
        }
      } catch (Browser.Retry r) {
        throw r;
      } catch (Browser.Fatal f) {
        throw f;
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return null;
  }

  private static int getThread() {
    synchronized (doneLock) {
      while (true) {
        for (int i = 0; i < WebApp.THREADS; i++) {
          if (done[i]) {
            done[i] = false;
            return i;
          }
        }
        try {
          doneLock.wait();
        } catch (InterruptedException e) {}
      }
    }
  }

  public static String get(Fetch fetch, Request req) {
    if (!isUrlValid(fetch.url)) {
      return null;
    }
    final int myThread = getThread();
    if (!req.continueSession) {
      restart(req, !CommonUtil.isEmpty(fetch.media), myThread);
    }
    Log.info("Get URL " + fetch.url + ". Cached: " + fetch.fetchCached, false);
    String resp = "";
    try {
      resp = getHelper(browsers.get()[myThread], fetch.throttle, null, fetch.url, fetch.fetchCached,
          req.runGuid, true, true, fetch.postFetchClicks);
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    } finally {
      synchronized (doneLock) {
        done[myThread] = true;
        doneLock.notify();
      }
    }
    return resp;
  }

  private static SearchResults filterResults(SearchResults results, String[] whitelist,
      String[] patterns, HtmlNode[] urlNodes, UrlTransform[] urlTransforms, boolean forExport) {
    if (results == null) {
      return SearchResults.newInstance(true);
    }
    SearchResults ret;
    results = UrlUtil.transformUrls(results, urlTransforms, forExport);
    if ((whitelist == null || whitelist.length == 0)
        && (patterns == null || patterns.length == 0)
        && (urlNodes == null || urlNodes.length == 0)) {
      ret = results;
    } else {
      List<Result> filtered = new ArrayList<Result>();
      for (int i = 0; i < results.size(); i++) {
        if (!NodeUtil.isResultFiltered(results.get(i), whitelist, patterns, urlNodes)) {
          filtered.add(results.get(i));
        }
      }
      if (filtered.isEmpty() && !results.isEmpty()) {
        Log.warn("Filtered every url, e.g., " + results.get(0).url);
      }
      ret = SearchResults.newInstance(true, filtered, results);
    }
    return ret;
  }

  public static List<HtmlNode> loadForm(FormLoad formLoad, Request req) throws ActionFailed {
    if (!isUrlValid(formLoad.site)) {
      return new ArrayList<HtmlNode>();
    }
    final int myThread = getThread();
    if (!req.continueSession) {
      restart(req, false, myThread);
    }
    try {
      List<HtmlNode> ret = null;
      try {
        ret = QueryForm.load(browsers.get()[myThread], formLoad, true);
      } catch (Browser.Retry r) {
        throw r;
      } catch (Browser.Fatal f) {
        throw f;
      } catch (Throwable t) {
        if (!req.continueSession) {
          restart(req, false, myThread);
        }
        ret = QueryForm.load(browsers.get()[myThread], formLoad, true);
      }
      return ret;
    } finally {
      synchronized (doneLock) {
        done[myThread] = true;
        doneLock.notify();
      }
    }
  }

  private static class Context {
    private Request req;
    private Query query;
    private Query recQuery;
    private int page;
    private int depth;
    private SearchResults allResults;
    private SearchResults newResults;
    private SearchResults recResults;
    private List<String> resultPages;
    private boolean media;
    private Map<String, Object> cache;
    private int threadNum;
  }

  private static void handlePage(Context context) throws ActionFailed, End {
    if (context.query.extract) {
      if (context.newResults.isEmpty()) {
        SearchResults tmpResults;
        try {
          tmpResults = ProcessPage.perform(browsers.get()[context.threadNum],
              context.page, context.query, context.threadNum);
        } catch (Browser.Retry r) {
          SearchResults.revalidate(browsers.get()[context.threadNum], true, context.threadNum);
          tmpResults = ProcessPage.perform(browsers.get()[context.threadNum],
              context.page, context.query, context.threadNum);
        }
        tmpResults = filterResults(tmpResults, context.query.urlWhitelist, context.query.urlPatterns,
            context.query.urlMatchNodes, context.query.urlTransforms, false);
        if (context.allResults.isDuplicatePage(tmpResults)) {
          throw new End();
        }
        if (context.query.results > 0
            && context.allResults.size() + tmpResults.size() > context.query.results) {
          int remove = context.allResults.size() + tmpResults.size() - context.query.results;
          for (int i = 0; i < remove && !tmpResults.isEmpty(); i++) {
            tmpResults.remove(tmpResults.size() - 1);
          }
        }
        context.newResults.addPage(tmpResults);
      }
      if (context.query.fetch) {
        fetch(browsers.get()[context.threadNum], context);
      }
      if (context.query.collapse) {
        for (int i = 0; i < context.newResults.size(); i++) {
          context.newResults.get(i).close();
        }
      }
      context.allResults.addPage(context.newResults);
    } else {
      context.resultPages.add(NodeUtil.clean(browsers.get()[context.threadNum].getPageSource(),
          browsers.get()[context.threadNum].getCurrentUrl()).outerHtml());
    }
  }

  public static List<Result> scrape(Query query, Request req) {
    final int myThread = getThread();
    boolean media = hasMedia(query);
    if (!req.continueSession) {
      restart(req, media, myThread);
    }
    try {
      Map<String, Object> cache = new HashMap<String, Object>();
      SearchResults ret = null;
      for (int i = 0; i < MAX_INIT; i++) {
        try {
          ret = scrape(query, req, 0, i + 1 == MAX_INIT, media, cache, myThread);
          Log.info("Scrape finished");
          List<Result> searchResults = ret.drain();
          for (Result result : searchResults) {
            if (result.isClosed()) {
              Result.addHold();
            }
          }
          return searchResults;
        } catch (Browser.Fatal f) {
          Log.exception(f);
          Log.warn("Reinitializing state and resuming scrape...");
          restart(req, media, myThread);
        }
      }
      return null;
    } finally {
      synchronized (doneLock) {
        done[myThread] = true;
        doneLock.notify();
      }
    }
  }

  private static boolean hasMedia(Query query) {
    Query cur = query;
    while (cur != null) {
      if (!CommonUtil.isEmpty(cur.media) || cur.allMedia) {
        return true;
      }
      cur = query.keywordQuery == null ? query.formQuery : query.keywordQuery;
    }
    return false;
  }

  private static SearchResults scrape(Query query, Request req, int depth,
      boolean fallback, boolean media, Map<String, Object> cache, int threadNum) {
    Context context = new Context();
    context.req = req;
    context.query = query;
    context.recQuery = query.keywordQuery == null
        ? (query.formQuery == null ? null : query.formQuery) : query.keywordQuery;
    context.depth = depth;
    context.media = media;
    context.cache = cache;
    context.threadNum = threadNum;
    if (cache.containsKey(Integer.toString(depth))) {
      Map<String, Object> curCache = (Map<String, Object>) cache.get(Integer.toString(depth));
      context.allResults = (SearchResults) curCache.get("results");
      context.recResults = (SearchResults) curCache.get("recResults");
      context.resultPages = (List<String>) curCache.get("resultPages");
    } else {
      Map<String, Object> curCache = new HashMap<String, Object>();
      cache.put(Integer.toString(depth), curCache);
      context.allResults = SearchResults.newInstance(false);
      curCache.put("results", context.allResults);
      context.recResults = SearchResults.newInstance(false);
      curCache.put("recResults", context.recResults);
      context.resultPages = new ArrayList<String>();
      curCache.put("resultPages", context.resultPages);
    }
    try {
      if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
        throw new Cancelled();
      }
      if (query.isFormQuery()) {
        Log.info("FormQuery for URL " + query.site, false);
        try {
          QueryForm.perform(browsers.get()[threadNum], (FormQuery) query, depth == 0);
        } catch (Throwable e) {
          if (depth == 0) {
            restart(req, media, threadNum);
          }
          QueryForm.perform(browsers.get()[threadNum], (FormQuery) query, depth == 0);
        }
      } else {
        Log.info("KewordQuery for URL " + query.site + ". Query: " + ((KeywordQuery) query).keywords, false);
        try {
          QueryKeyword.perform(browsers.get()[threadNum], (KeywordQuery) query, depth == 0);
        } catch (Throwable e) {
          if (depth == 0) {
            restart(req, media, threadNum);
          }
          QueryKeyword.perform(browsers.get()[threadNum], (KeywordQuery) query, depth == 0);
        }
      }
      if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
        throw new Cancelled();
      }
      String priorProceedLabel = null;
      for (int page = 1; (page <= query.pages || query.pages <= 0)
          && (context.allResults.size() < query.results || query.results <= 0); page++) {
        if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
          throw new Cancelled();
        }
        if (page > 1) {
          if (!query.fetch) {
            try {
              BrowserUtil.browserSleepLong(query.throttle);
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
          Log.info("Proceeding to page " + page);
          try {
            priorProceedLabel = Proceed.perform(browsers.get()[threadNum],
                query.proceedClicks, page, priorProceedLabel);
          } catch (Browser.Retry r) {
            SearchResults.revalidate(browsers.get()[threadNum], true, threadNum);
            priorProceedLabel = Proceed.perform(browsers.get()[threadNum],
                query.proceedClicks, page, priorProceedLabel);
          }
          if (ScreenSlicerBatch.isCancelled(req.runGuid)) {
            throw new Cancelled();
          }
        }
        if (query.currentPage() + 1 == page) {
          context.page = page;
          context.newResults = SearchResults.newInstance(true);
          try {
            handlePage(context);
          } catch (Browser.Retry r) {
            SearchResults.revalidate(browsers.get()[threadNum], true, threadNum);
            handlePage(context);
          }
          query.markPage(page);
          query.markResult(0);
        }
      }
      query.markPage(0);
    } catch (End e) {
      Log.info("Reached end of results", false);
    } catch (Cancelled c) {
      Log.info("Cancellation requested.");
    } catch (Throwable t) {
      if (fallback) {
        Log.warn("Too many errors. Finishing scrape...");
      } else {
        throw new Browser.Fatal(t);
      }
    }
    cache.remove(Integer.toString(depth));
    if (query.extract) {
      if (context.recResults.isEmpty()) {
        return filterResults(context.allResults, query.urlWhitelist,
            query.urlPatterns, query.urlMatchNodes, query.urlTransforms, true);
      }
      if (query.collapse) {
        for (int i = 0; i < context.allResults.size(); i++) {
          context.allResults.get(i).remove();
        }
      }
      return context.recResults;
    }
    List<Result> pages = new ArrayList<Result>();
    for (String page : context.resultPages) {
      Result r = new Result();
      r.html = page;
      pages.add(r);
    }
    return SearchResults.newInstance(false, pages, null);
  }

  private static boolean isUrlValid(String url) {
    return !CommonUtil.isEmpty(url) && (url.startsWith("https://") || url.startsWith("http://"));
  }

  public static List<Result> scrape(String url, final String query,
      final int pages, final String mapKey1, final String mapKey2) {
    if (!isUrlValid(url)) {
      return new ArrayList<Result>();
    }
    synchronized (doneLock) {
      if (!done[0]) {
        return null;
      }
      done[0] = false;
    }
    restart(new Request(), false, 0);
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
      QueryKeyword.perform(browsers.get()[0], keywordQuery, true);
      synchronized (progressLock) {
        progress1 = "Page 1 progress: extracting results...";
      }
      results.addAll(ProcessPage.perform(browsers.get()[0], 1, keywordQuery, 0).drain());
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
      synchronized (doneLock) {
        done[0] = true;
      }
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
      synchronized (doneLock) {
        done[0] = true;
      }
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
          Proceed.perform(browsers.get()[0], null, 2, query);
          synchronized (progressLock) {
            progress2 = "Page 2 progress: extracting results...";
          }
          next.addAll(ProcessPage.perform(browsers.get()[0], 2, keywordQuery, 0).drain());
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
          synchronized (doneLock) {
            done[0] = true;
          }
        }
      }
    }).start();
    return results;
  }
}
