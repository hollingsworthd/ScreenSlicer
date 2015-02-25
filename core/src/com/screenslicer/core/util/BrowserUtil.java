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
package com.screenslicer.core.util;

import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.machinepublishers.browser.Browser;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.webapp.WebApp;

public class BrowserUtil {
  private static final int REFRESH_TRIES = 3;
  private static final String NODE_MARKER = "fftheme_";
  private static final String HIDDEN_MARKER = "xmoztheme";
  private static final String FILTERED_MARKER = "o2xtheme";
  private static final String FILTERED_LENIENT_MARKER = "o2x2theme";
  private static final String isVisible =
      "      function isCurrentlyVisible(element, rect) {"
          + "  var atPoint,"
          + "  docElement = document.documentElement,"
          + "  clientWidth = docElement.clientWidth,"
          + "  clientHeight = docElement.clientHeight,"
          + "  fromPoint = function (x, y) { return document.elementFromPoint(x, y) },"
          + "  contains = \"contains\" in element ? \"contains\" : \"compareDocumentPosition\","
          + "  has = contains === \"contains\" ? true : 0x14;"
          + "  if(rect.right < 0 || rect.bottom < 0 || rect.left > clientWidth || rect.top > clientHeight)"
          + "    return false;"
          + "  return (((atPoint = fromPoint(rect.left, rect.top)) === element "
          + "  || element[contains](atPoint) === has)"
          + "    || ((atPoint = fromPoint((rect.left+rect.right)/2, rect.top)) === element "
          + "  || element[contains](atPoint) === has)"
          + "    || ((atPoint = fromPoint(rect.left, (rect.top+rect.bottom)/2)) === element "
          + "  || element[contains](atPoint) === has)"
          + "    || ((atPoint = fromPoint((rect.left+rect.right)/2, (rect.top+rect.bottom)/2)) === element "
          + "  || element[contains](atPoint) === has));"
          + "}"
          + "function isVisible(element) {"
          + "  element.scrollIntoView();"
          + "  return isCurrentlyVisible(element, element.getBoundingClientRect());"
          + "}";
  private static int STARTUP_WAIT_MS = 100;
  private static int LONG_WAIT_MS = 5837;
  private static int SHORT_WAIT_MS = 1152;
  private static int SHORT_WAIT_MIN_MS = 3783;
  private static int VERY_SHORT_WAIT_MS = 381;
  private static int VERY_SHORT_WAIT_MIN_MS = 327;
  private static int RAND_MIN_WAIT_MS = 7734;
  private static int RAND_WAIT_MS = 3734;
  private static int RAND_MAX_WAIT_MS = 129 * 1000;
  private static int RAND_MIN_WAIT_ITER = 1;
  private static int RAND_MAX_WAIT_ITER = 5;
  private static final SecureRandom rand = new SecureRandom();

  public static void browserSleepStartup() {
    try {
      Thread.sleep(STARTUP_WAIT_MS);
    } catch (InterruptedException e) {}
  }

  public static void browserSleepRand(boolean longSleep) {
    if (longSleep) {
      try {
        int cur = RAND_MAX_WAIT_MS;
        int iter = rand.nextInt(RAND_MAX_WAIT_ITER) + RAND_MIN_WAIT_ITER;
        for (int i = 0; i < iter; i++) {
          cur = rand.nextInt(cur + 1);
        }
        Thread.sleep(rand.nextInt(cur + 1) + rand.nextInt(RAND_WAIT_MS) + RAND_MIN_WAIT_MS);
      } catch (InterruptedException e) {}
    } else {
      try {
        Thread.sleep(RAND_MIN_WAIT_MS / 2);
      } catch (InterruptedException e) {}
    }
  }

  public static void browserSleepShort() {
    try {
      Thread.sleep(rand.nextInt(SHORT_WAIT_MS) + SHORT_WAIT_MIN_MS);
    } catch (InterruptedException e) {}
  }

  public static void browserSleepVeryShort() {
    try {
      Thread.sleep(rand.nextInt(VERY_SHORT_WAIT_MS) + VERY_SHORT_WAIT_MIN_MS);
    } catch (InterruptedException e) {}
  }

  public static void browserSleepLong() {
    try {
      Thread.sleep(LONG_WAIT_MS);
    } catch (InterruptedException e) {}
  }

  public static boolean statusFail(int statusCode) {
    return statusCode < 200 || (statusCode > 299 && statusCode != 304);
  }

  public static void get(Browser browser, String url, Node urlNode, boolean reAttempt, boolean toNewWindow, boolean cleanupWindows) throws ActionFailed {
    if (CommonUtil.isEmpty(url) && urlNode == null) {
      throw new ActionFailed();
    }
    boolean exception = true;
    boolean success = true;
    String origHandle = null;
    try {
      String switchTo = null;
      String source = null;
      boolean badUrl = true;
      boolean statusFail = true;
      origHandle = browser.getWindowHandle();
      Log.debug("Orig handle: " + origHandle, WebApp.DEBUG);
      Set<String> handlesBefore = browser.getWindowHandles();
      for (int i = 0; i < REFRESH_TRIES
          && (badUrl || statusFail || exception || CommonUtil.isEmpty(source)); i++) {
        switchTo = null;
        boolean terminate = false;
        try {
          badUrl = false;
          statusFail = false;
          exception = false;
          source = null;
          Log.debug("getting url...", WebApp.DEBUG);
          try {
            browser.getKeyboard().sendKeys(Keys.ESCAPE);
          } catch (Browser.Retry r) {
            throw r;
          } catch (Browser.Fatal f) {
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
          }
          if (urlNode != null) {
            try {
              handleNewWindows(browser, origHandle, cleanupWindows);
            } catch (Browser.Retry r) {
              throw r;
            } catch (Browser.Fatal f) {
              throw f;
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
          try {
            browserSleepVeryShort();
            if (urlNode != null) {
              try {
                Set<String> origHandles = browser.getWindowHandles();
                click(browser, toElement(browser, urlNode), toNewWindow);
                Set<String> newHandles = browser.getWindowHandles();
                switchTo = origHandle;
                for (String newHandle : newHandles) {
                  if (!origHandles.contains(newHandle) && !origHandle.equals(newHandle)) {
                    switchTo = newHandle;
                  }
                }
                for (String newHandle : newHandles) {
                  if (!origHandles.contains(newHandle) && !newHandle.equals(switchTo)) {
                    browser.switchTo().window(newHandle);
                    browser.close();
                  }
                }
                if (switchTo != null) {
                  Log.debug("Switching to: " + switchTo, WebApp.DEBUG);
                  browser.switchTo().window(switchTo);
                }
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (Throwable t) {
                exception = true;
                Log.exception(t);
                handleNewWindows(browser, origHandle, cleanupWindows);
              }
            } else if (!CommonUtil.isEmpty(url)) {
              browser.get("about:blank");
              try {
                browser.get(url);
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (TimeoutException e) {
                Log.exception(e);
              }
            }
            if (!exception) {
              browserSleepShort();
              browserSleepLong();
              //TODO wait for long ajax requests
              statusFail = statusFail(browser.getStatusCode());
              browser.switchTo().defaultContent();
              source = browser.getPageSource();
              try {
                new URL(browser.getCurrentUrl());
                badUrl = false;
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (Throwable t) {
                badUrl = true;
              }
            }
          } catch (Browser.Retry r) {
            terminate = true;
            throw r;
          } catch (Browser.Fatal f) {
            terminate = true;
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
            exception = true;
          }
          if (badUrl || statusFail || exception || CommonUtil.isEmpty(source)) {
            switchTo = null;
            if (!reAttempt || i + 1 == REFRESH_TRIES) {
              try {
                browser.getKeyboard().sendKeys(Keys.ESCAPE);
                browserSleepVeryShort();
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (Throwable t) {
                Log.exception(t);
              }
              success = false;
              if (!reAttempt) {
                break;
              }
            }
          }
        } finally {
          if (!terminate) {
            Set<String> handlesAfter = browser.getWindowHandles();
            for (String curHandle : handlesAfter) {
              if (!handlesBefore.contains(curHandle) && !curHandle.equals(switchTo)) {
                browser.switchTo().window(curHandle);
                browser.close();
              }
            }
            browser.switchTo().window(switchTo == null ? origHandle : switchTo);
            browser.switchTo().defaultContent();
          }
        }
      }
      Log.debug("getting url - done", WebApp.DEBUG);
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
      success = false;
    }
    if (!success) {
      if (urlNode != null && origHandle != null) {
        try {
          handleNewWindows(browser, origHandle, cleanupWindows);
        } catch (Browser.Retry r) {
          throw r;
        } catch (Browser.Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
      throw new ActionFailed();
    }
  }

  public static void get(Browser browser, String url, boolean retry, boolean cleanupWindows) throws ActionFailed {
    get(browser, url, null, retry, true, cleanupWindows);
  }

  public static String newWindow(Browser browser, boolean cleanupWindows) throws ActionFailed {
    try {
      handleNewWindows(browser, browser.getWindowHandle(), cleanupWindows);
      Set<String> origHandles = new HashSet<String>(browser.getWindowHandles());
      try {
        browser.getKeyboard().sendKeys(Keys.chord(Keys.CONTROL + "n"));
      } catch (Browser.Retry r) {
        throw r;
      } catch (Browser.Fatal f) {
        throw f;
      } catch (Throwable t) {
        Log.exception(t);
      }
      browserSleepStartup();
      Collection<String> handles = new HashSet<String>(browser.getWindowHandles());
      handles.removeAll(origHandles);
      if (!handles.isEmpty()) {
        browser.switchTo().window(handles.iterator().next());
      } else {
        browser.executeScript("window.open('');");
        browserSleepStartup();
        handles = new HashSet<String>(browser.getWindowHandles());
        handles.removeAll(origHandles);
        if (!handles.isEmpty()) {
          browser.switchTo().window(handles.iterator().next());
        }
      }
      return browser.getWindowHandle();
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static void handleNewWindows(Browser browser, String handleToKeep, boolean cleanup) throws ActionFailed {
    try {
      if (cleanup) {
        Set<String> handles = new HashSet<String>(browser.getWindowHandles());
        for (String handle : handles) {
          try {
            if (!handleToKeep.equals(handle)) {
              browser.switchTo().window(handle);
              browser.close();
            }
          } catch (Browser.Retry r) {
            throw r;
          } catch (Browser.Fatal f) {
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      }
      browser.switchTo().window(handleToKeep);
      browser.switchTo().defaultContent();
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static Element openElement(final Browser browser, boolean init, final String[] whitelist,
      final String[] patterns, final HtmlNode[] urlNodes, final UrlTransform[] transforms)
      throws ActionFailed {

    try {
      if (init) {
        browser.executeScript(
            "      var all = document.body.getElementsByTagName('*');"
                + "for(var i = 0; i < all.length; i++){"
                + "  if(all[i].className && typeof all[i].className == 'string'){"
                + "    all[i].className=all[i].className.replace(/"
                + NODE_MARKER + "\\d+/g,'').replace(/"
                + HIDDEN_MARKER + "/g,'').replace(/"
                + FILTERED_MARKER + "/g,'').replace(/"
                + FILTERED_LENIENT_MARKER + "/g,'').replace(/\\s+/g,' ').trim();"
                + "  }"
                + "}"
                + isVisible
                + "for(var j = 0; j < all.length; j++){"
                + "  all[j].className += ' " + NODE_MARKER + "'+j+' ';"
                + "  if(!isVisible(all[j])){"
                + "    all[j].className += ' " + HIDDEN_MARKER + " ';"
                + "  }"
                + "}");
      }
      String url = browser.getCurrentUrl();
      new URL(url);
      Element element = CommonUtil.parse(browser.getPageSource(), url, false).body();
      element.traverse(new NodeVisitor() {
        @Override
        public void tail(Node node, int depth) {}

        @Override
        public void head(Node node, int depth) {
          if (!node.nodeName().equals("#text") && !NodeUtil.isEmpty(node)) {
            NodeUtil.markVisible(node);
          }
        }
      });
      if ((whitelist != null && whitelist.length > 0)
          || (patterns != null && patterns.length > 0)
          || (urlNodes != null && urlNodes.length > 0)) {
        element.traverse(new NodeVisitor() {
          @Override
          public void tail(Node node, int depth) {}

          @Override
          public void head(Node node, int depth) {
            if (node.nodeName().equals("a")) {
              if (UrlUtil.isUrlFiltered(browser.getCurrentUrl(), node.attr("href"), node, whitelist, patterns, urlNodes, transforms)) {
                NodeUtil.markFiltered(node, false);
              }
            } else {
              String urlAttr = UrlUtil.urlFromAttr(node);
              if (!CommonUtil.isEmpty(urlAttr)
                  && UrlUtil.isUrlFiltered(browser.getCurrentUrl(), urlAttr, node, whitelist, patterns, urlNodes, transforms)) {
                NodeUtil.markFiltered(node, true);
              }
            }
          }
        });
      }
      return element;
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static boolean click(Browser browser, WebElement toClick, boolean shift) {
    try {
      Actions action = new Actions(browser);
      BrowserUtil.browserSleepVeryShort();
      action.moveToElement(toClick).perform();
      BrowserUtil.browserSleepVeryShort();
      if (shift) {
        browser.getKeyboard().pressKey(Keys.SHIFT);
      }
      toClick.click();
      if (shift) {
        browser.getKeyboard().releaseKey(Keys.SHIFT);
      }
      BrowserUtil.browserSleepVeryShort();
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      return false;
    }
    return true;
  }

  public static boolean doClicks(Browser browser, HtmlNode[] controls, Element body, Boolean toNewWindow) throws ActionFailed {
    boolean clicked = false;
    if (controls != null && controls.length > 0) {
      Log.debug("Doing clicks", WebApp.DEBUG);
      if (body == null) {
        body = BrowserUtil.openElement(browser, true, null, null, null, null);
      }
      for (int i = 0; i < controls.length; i++) {
        if (!CommonUtil.isEmpty(controls[i].httpGet)) {
          BrowserUtil.get(browser, controls[i].httpGet, true, false);
          continue;
        }
        if (i > 0 && (controls[i - 1].longRequest || !CommonUtil.isEmpty(controls[i - 1].httpGet))) {
          body = BrowserUtil.openElement(browser, true, null, null, null, null);
        }
        WebElement element = BrowserUtil.toElement(browser, controls[i], body);
        if (WebApp.DEBUG) {
          Log.debug("click - " + controls[i], WebApp.DEBUG);
          String found = null;
          try {
            found = element == null ? null : CommonUtil.strip(element.getAttribute("outerHTML"), false);
          } catch (Throwable t) {
            Log.exception(t);
          }
          Log.debug("click found - " + found, WebApp.DEBUG);
        }
        if (element != null) {
          clicked = true;
          click(browser, element, toNewWindow == null ? controls[i].newWindow : toNewWindow);
          if (controls[i].longRequest) {
            //TODO wait for long ajax requests
          }
        }
      }
    } else {
      Log.debug("No clicks to perform", WebApp.DEBUG);
    }
    return clicked;
  }

  public static WebElement toElement(Browser browser, Node node) {
    if (node == null) {
      return null;
    }
    try {
      String classId = NodeUtil.classId(node);
      if (classId != null) {
        return browser.findElementByClassName(classId);
      }
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    Log.warn("Could not convert Node to WebElement... trying fuzzy search");
    try {
      HtmlNode find = new HtmlNode();
      Element body = BrowserUtil.openElement(browser, false, null, null, null, null);
      find.alt = node.attr("alt");
      find.classes = CommonUtil.isEmpty(node.attr("class")) ? null : node.attr("class").split("\\s");
      find.href = node.attr("href");
      find.id = node.attr("id");
      find.innerText = node instanceof Element ? ((Element) node).text() : null;
      find.name = node.attr("name");
      find.tagName = node.nodeName();
      find.title = node.attr("title");
      find.type = node.attr("type");
      find.value = node.attr("value");
      find.fuzzy = true;
      WebElement found = toElement(browser, find, body);
      if (found != null) {
        return found;
      }
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    Log.warn("Could not convert Node to WebElement... failed permanently");
    return null;
  }

  public static WebElement toElement(Browser browser, HtmlNode htmlNode, Element body) throws ActionFailed {
    if (body == null) {
      body = BrowserUtil.openElement(browser, true, null, null, null, null);
    }
    if (!CommonUtil.isEmpty(htmlNode.id)) {
      WebElement element = toElement(browser, body.getElementById(htmlNode.id));
      if (element != null) {
        return element;
      }
    }
    List<Elements> selected = new ArrayList<Elements>();
    if (!CommonUtil.isEmpty(htmlNode.tagName)) {
      selected.add(body.getElementsByTag(htmlNode.tagName));
    } else if (!CommonUtil.isEmpty(htmlNode.href)) {
      selected.add(body.getElementsByTag("a"));
    }
    if (!CommonUtil.isEmpty(htmlNode.name)) {
      selected.add(body.getElementsByAttributeValue("name", htmlNode.name));
    }
    if (!CommonUtil.isEmpty(htmlNode.type)) {
      selected.add(body.getElementsByAttributeValue("type", htmlNode.type));
    }
    if (!CommonUtil.isEmpty(htmlNode.value)) {
      selected.add(body.getElementsByAttributeValue("value", htmlNode.value));
    }
    if (!CommonUtil.isEmpty(htmlNode.title)) {
      selected.add(body.getElementsByAttributeValue("title", htmlNode.title));
    }
    if (!CommonUtil.isEmpty(htmlNode.alt)) {
      selected.add(body.getElementsByAttributeValue("alt", htmlNode.alt));
    }
    if (htmlNode.classes != null && htmlNode.classes.length > 0) {
      Map<Element, Integer> found = new HashMap<Element, Integer>();
      for (int i = 0; i < htmlNode.classes.length; i++) {
        Elements elements = body.getElementsByClass(htmlNode.classes[i]);
        for (Element element : elements) {
          if (!found.containsKey(element)) {
            found.put(element, 0);
          }
          found.put(element, found.get(element) + 1);
        }
      }
      Elements elements = new Elements();
      for (int i = htmlNode.classes.length; i > 0; i--) {
        for (Map.Entry<Element, Integer> entry : found.entrySet()) {
          if (entry.getValue() == i) {
            elements.add(entry.getKey());
          }
        }
        if (!elements.isEmpty()) {
          break;
        }
      }
      selected.add(elements);
    }
    if (!CommonUtil.isEmpty(htmlNode.href)) {
      Elements hrefs = body.getElementsByAttribute("href");
      Elements toAdd = new Elements();
      String currentUrl = browser.getCurrentUrl();
      String hrefGiven = htmlNode.href;
      for (Element href : hrefs) {
        String hrefFound = href.attr("href");
        if (hrefGiven.equalsIgnoreCase(hrefFound)) {
          toAdd.add(href);
          toAdd.add(href);
          toAdd.add(href);
        } else if (htmlNode.fuzzy && hrefFound != null && hrefFound.endsWith(hrefGiven)) {
          toAdd.add(href);
          toAdd.add(href);
        } else if (htmlNode.fuzzy && hrefFound != null && hrefFound.contains(hrefGiven)) {
          toAdd.add(href);
        } else {
          String uriGiven = UrlUtil.toCanonicalUri(currentUrl, hrefGiven);
          String uriFound = UrlUtil.toCanonicalUri(currentUrl, hrefFound);
          if (uriGiven.equalsIgnoreCase(uriFound)) {
            toAdd.add(href);
          }
        }
      }
      selected.add(toAdd);
    }
    if (!CommonUtil.isEmpty(htmlNode.innerText)) {
      selected.add(body.getElementsMatchingText(Pattern.quote(htmlNode.innerText)));
    }
    if (htmlNode.multiple != null) {
      selected.add(body.getElementsByAttribute("multiple"));
    }
    Map<Element, Integer> votes = new HashMap<Element, Integer>();
    for (Elements elements : selected) {
      for (Element element : elements) {
        if (!votes.containsKey(element)) {
          votes.put(element, 0);
        }
        votes.put(element, votes.get(element) + 2);
        if (!NodeUtil.isHidden(element)) {
          votes.put(element, votes.get(element) + 1);
        }
      }
    }
    int maxVote = 0;
    Element maxElement = null;
    for (Map.Entry<Element, Integer> entry : votes.entrySet()) {
      if (entry.getValue() > maxVote) {
        maxVote = entry.getValue();
        maxElement = entry.getKey();
      }
    }
    return toElement(browser, maxElement);
  }
}
