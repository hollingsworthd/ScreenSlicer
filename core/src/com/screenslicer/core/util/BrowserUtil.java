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
import org.openqa.selenium.remote.BrowserDriver;
import org.openqa.selenium.remote.BrowserDriver.Fatal;
import org.openqa.selenium.remote.BrowserDriver.Retry;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.UrlTransform;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.service.HttpStatus;
import com.screenslicer.webapp.WebApp;

public class BrowserUtil {
  private static final int REFRESH_TRIES = 3;
  private static final int LONG_REQUEST_WAIT = 10000;
  private static final String NODE_MARKER = "fftheme_";
  private static final String HIDDEN_MARKER = "xmoztheme";
  private static final String FILTERED_MARKER = "o2xtheme";
  private static final String FILTERED_LENIENT_MARKER = "o2x2theme";
  /*
   * used because WebElement.isDisplayed() is way too slow
   */
  private static final String isVisible =
      "      function isCurrentlyVisible(el, rect) {"
          + "  var eap,"
          + "  docEl = document.documentElement,"
          + "  vWidth = docEl.clientWidth,"
          + "  vHeight = docEl.clientHeight,"
          + "  efp = function (x, y) { return document.elementFromPoint(x, y) },"
          + "  contains = \"contains\" in el ? \"contains\" : \"compareDocumentPosition\","
          + "  has = contains == \"contains\" ? 1 : 0x14;"
          + "  if(rect.right < 0 || rect.bottom < 0 || rect.left > vWidth || rect.top > vHeight)"
          + "    return false;"
          + "  return ((eap = efp(rect.left,  rect.top)) == el || el[contains](eap) == has"
          + "    || (eap = efp(rect.right, rect.top)) == el || el[contains](eap) == has"
          + "    || (eap = efp(rect.right, rect.bottom)) == el || el[contains](eap) == has"
          + "    || (eap = efp(rect.left,  rect.bottom)) == el || el[contains](eap) == has"
          + "    || (eap = efp((rect.left+rect.right)/2,  rect.top)) == el || el[contains](eap) == has"
          + "    || (eap = efp((rect.left+rect.right)/2,  rect.bottom)) == el || el[contains](eap) == has"
          + "    || (eap = efp(rect.left,  (rect.top+rect.bottom)/2)) == el || el[contains](eap) == has"
          + "    || (eap = efp(rect.right,  (rect.top+rect.bottom)/2)) == el || el[contains](eap) == has"
          + "    || (eap = efp((rect.left+rect.right)/2,  (rect.top+rect.bottom)/2)) == el || el[contains](eap) == has);"
          + "}"
          + "function isVisible(element) {"
          + "  var rect = element.getBoundingClientRect();"
          + "  window.scrollBy(rect.left, rect.top);"
          + "  rect = element.getBoundingClientRect();"
          + "  if(isCurrentlyVisible(element, rect)){"
          + "    return true;"
          + "  }"
          + "  window.scrollByLines(-10);"
          + "  rect = element.getBoundingClientRect();"
          + "  return isCurrentlyVisible(element, rect);"
          + "}";
  private static int STARTUP_WAIT_MS = 100;
  private static int LONG_WAIT_MS = 5837;
  private static int RESET_WAIT_MS = 180000;
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

  public static void driverSleepStartup() {
    try {
      Thread.sleep(STARTUP_WAIT_MS);
    } catch (InterruptedException e) {}
  }

  public static void driverSleepReset() {
    try {
      Thread.sleep(RESET_WAIT_MS);
    } catch (InterruptedException e) {}
  }

  public static void driverSleepRand(boolean longSleep) {
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

  public static void driverSleepShort() {
    try {
      Thread.sleep(rand.nextInt(SHORT_WAIT_MS) + SHORT_WAIT_MIN_MS);
    } catch (InterruptedException e) {}
  }

  public static void driverSleepVeryShort() {
    try {
      Thread.sleep(rand.nextInt(VERY_SHORT_WAIT_MS) + VERY_SHORT_WAIT_MIN_MS);
    } catch (InterruptedException e) {}
  }

  public static void driverSleepLong() {
    try {
      Thread.sleep(LONG_WAIT_MS);
    } catch (InterruptedException e) {}
  }

  public static void get(BrowserDriver driver, String url, Node urlNode, boolean reAttempt, boolean toNewWindow, boolean cleanupWindows) throws ActionFailed {
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
      origHandle = driver.getWindowHandle();
      Log.debug("Orig handle: " + origHandle, WebApp.DEBUG);
      Set<String> handlesBefore = driver.getWindowHandles();
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
            driver.getKeyboard().sendKeys(Keys.ESCAPE);
          } catch (Retry r) {
            throw r;
          } catch (Fatal f) {
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
          }
          if (urlNode != null) {
            try {
              handleNewWindows(driver, origHandle, cleanupWindows);
            } catch (Retry r) {
              throw r;
            } catch (Fatal f) {
              throw f;
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
          try {
            driverSleepVeryShort();
            if (urlNode != null) {
              try {
                Set<String> origHandles = driver.getWindowHandles();
                click(driver, toElement(driver, urlNode), toNewWindow);
                Set<String> newHandles = driver.getWindowHandles();
                switchTo = origHandle;
                for (String newHandle : newHandles) {
                  if (!origHandles.contains(newHandle) && !origHandle.equals(newHandle)) {
                    switchTo = newHandle;
                  }
                }
                for (String newHandle : newHandles) {
                  if (!origHandles.contains(newHandle) && !newHandle.equals(switchTo)) {
                    driver.switchTo().window(newHandle);
                    driver.close();
                  }
                }
                if (switchTo != null) {
                  Log.debug("Switching to: " + switchTo, WebApp.DEBUG);
                  driver.switchTo().window(switchTo);
                }
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
                throw f;
              } catch (Throwable t) {
                exception = true;
                Log.exception(t);
                handleNewWindows(driver, origHandle, cleanupWindows);
              }
            } else if (!CommonUtil.isEmpty(url)) {
              driver.get("about:blank");
              try {
                driver.get(url);
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
                throw f;
              } catch (TimeoutException e) {
                Log.exception(e);
              }
            }
            if (!exception) {
              driverSleepShort();
              driverSleepLong();
              statusFail = HttpStatus.status(driver, urlNode != null || url != null) != 200;
              driver.switchTo().defaultContent();
              source = driver.getPageSource();
              try {
                new URL(driver.getCurrentUrl());
                badUrl = false;
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
                throw f;
              } catch (Throwable t) {
                badUrl = true;
              }
            }
          } catch (Retry r) {
            terminate = true;
            throw r;
          } catch (Fatal f) {
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
                driver.getKeyboard().sendKeys(Keys.ESCAPE);
                driverSleepVeryShort();
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
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
            Set<String> handlesAfter = driver.getWindowHandles();
            for (String curHandle : handlesAfter) {
              if (!handlesBefore.contains(curHandle) && !curHandle.equals(switchTo)) {
                driver.switchTo().window(curHandle);
                driver.close();
              }
            }
            driver.switchTo().window(switchTo == null ? origHandle : switchTo);
            driver.switchTo().defaultContent();
          }
        }
      }
      Log.debug("getting url - done", WebApp.DEBUG);
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
      success = false;
    }
    if (!success) {
      if (urlNode != null && origHandle != null) {
        try {
          handleNewWindows(driver, origHandle, cleanupWindows);
        } catch (Retry r) {
          throw r;
        } catch (Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
      throw new ActionFailed();
    }
  }

  public static void get(BrowserDriver driver, String url, boolean retry, boolean cleanupWindows) throws ActionFailed {
    get(driver, url, null, retry, true, cleanupWindows);
  }

  public static String newWindow(BrowserDriver driver, boolean cleanupWindows) throws ActionFailed {
    try {
      handleNewWindows(driver, driver.getWindowHandle(), cleanupWindows);
      Set<String> origHandles = new HashSet<String>(driver.getWindowHandles());
      try {
        driver.getKeyboard().sendKeys(Keys.chord(Keys.CONTROL + "n"));
      } catch (Retry r) {
        throw r;
      } catch (Fatal f) {
        throw f;
      } catch (Throwable t) {
        Log.exception(t);
      }
      driverSleepStartup();
      Collection<String> handles = new HashSet<String>(driver.getWindowHandles());
      handles.removeAll(origHandles);
      if (!handles.isEmpty()) {
        driver.switchTo().window(handles.iterator().next());
      } else {
        driver.executeScript("window.open('');");
        driverSleepStartup();
        handles = new HashSet<String>(driver.getWindowHandles());
        handles.removeAll(origHandles);
        if (!handles.isEmpty()) {
          driver.switchTo().window(handles.iterator().next());
        }
      }
      return driver.getWindowHandle();
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static void handleNewWindows(BrowserDriver driver, String handleToKeep, boolean cleanup) throws ActionFailed {
    try {
      if (cleanup) {
        Set<String> handles = new HashSet<String>(driver.getWindowHandles());
        for (String handle : handles) {
          try {
            if (!handleToKeep.equals(handle)) {
              driver.switchTo().window(handle);
              driver.close();
            }
          } catch (Retry r) {
            throw r;
          } catch (Fatal f) {
            throw f;
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      }
      driver.switchTo().window(handleToKeep);
      driver.switchTo().defaultContent();
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static Element openElement(final BrowserDriver driver, boolean init, final String[] whitelist,
      final String[] patterns, final HtmlNode[] urlNodes, final UrlTransform[] transforms)
      throws ActionFailed {
    try {
      if (init) {
        driver.executeScript(
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
      String url = driver.getCurrentUrl();
      new URL(url);
      Element element = CommonUtil.parse(driver.getPageSource(), url, false).body();
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
              if (UrlUtil.isUrlFiltered(driver.getCurrentUrl(), node.attr("href"), node, whitelist, patterns, urlNodes, transforms)) {
                NodeUtil.markFiltered(node, false);
              }
            } else {
              String urlAttr = UrlUtil.urlFromAttr(node);
              if (!CommonUtil.isEmpty(urlAttr)
                  && UrlUtil.isUrlFiltered(driver.getCurrentUrl(), urlAttr, node, whitelist, patterns, urlNodes, transforms)) {
                NodeUtil.markFiltered(node, true);
              }
            }
          }
        });
      }
      return element;
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static boolean click(BrowserDriver driver, WebElement toClick, boolean shift) {
    try {
      Actions action = driver.actions();
      driver.executeScript("arguments[0].scrollIntoView(false)", toClick);
      BrowserUtil.driverSleepVeryShort();
      action.moveToElement(toClick).perform();
      BrowserUtil.driverSleepVeryShort();
      String onClick = null;
      if (shift) {
        if (toClick.getTagName().equals("a")) {
          //disable onclick event--workaround https://bugzilla.mozilla.org/show_bug.cgi?id=151142
          onClick = toClick.getAttribute("onclick");
          onClick = CommonUtil.isEmpty(onClick) ? "" : (onClick.endsWith(";") ? onClick : onClick + ";");
          onClick = onClick.replaceAll("(?<!\\\\)'", "\\\\'");
          driver.executeScript("arguments[0].setAttribute('onclick','" + onClick
              + "if(event && event.stopPropagation) { event.stopPropagation(); }');", toClick);
        }
        driver.getKeyboard().pressKey(Keys.SHIFT);
      }
      toClick.click();
      if (shift) {
        if (toClick.getTagName().equals("a")) {
          driver.executeScript("arguments[0].setAttribute('onclick','" + onClick + "');", toClick);
        }
        driver.getKeyboard().releaseKey(Keys.SHIFT);
      }
      BrowserUtil.driverSleepVeryShort();
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      return false;
    }
    return true;
  }

  public static boolean doClicks(BrowserDriver driver, HtmlNode[] controls, Element body, Boolean toNewWindow) throws ActionFailed {
    boolean clicked = false;
    if (controls != null && controls.length > 0) {
      Log.debug("Doing clicks", WebApp.DEBUG);
      if (body == null) {
        body = BrowserUtil.openElement(driver, true, null, null, null, null);
      }
      for (int i = 0; i < controls.length; i++) {
        if (!CommonUtil.isEmpty(controls[i].httpGet)) {
          BrowserUtil.get(driver, controls[i].httpGet, true, false);
          continue;
        }
        if (i > 0 && (controls[i - 1].longRequest || !CommonUtil.isEmpty(controls[i - 1].httpGet))) {
          body = BrowserUtil.openElement(driver, true, null, null, null, null);
        }
        WebElement element = BrowserUtil.toElement(driver, controls[i], body);
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
          click(driver, element, toNewWindow == null ? controls[i].newWindow : toNewWindow);
          if (controls[i].longRequest) {
            HttpStatus.status(driver, LONG_REQUEST_WAIT);
          }
        }
      }
    } else {
      Log.debug("No clicks to perform", WebApp.DEBUG);
    }
    return clicked;
  }

  public static WebElement toElement(BrowserDriver driver, Node node) {
    if (node == null) {
      return null;
    }
    try {
      String classId = NodeUtil.classId(node);
      if (classId != null) {
        return driver.findElementByClassName(classId);
      }
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    Log.warn("Could not convert Node to WebElement... trying fuzzy search");
    try {
      HtmlNode find = new HtmlNode();
      Element body = BrowserUtil.openElement(driver, false, null, null, null, null);
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
      WebElement found = toElement(driver, find, body);
      if (found != null) {
        return found;
      }
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    Log.warn("Could not convert Node to WebElement... failed permanently");
    return null;
  }

  public static WebElement toElement(BrowserDriver driver, HtmlNode htmlNode, Element body) throws ActionFailed {
    if (body == null) {
      body = BrowserUtil.openElement(driver, true, null, null, null, null);
    }
    if (!CommonUtil.isEmpty(htmlNode.id)) {
      WebElement element = toElement(driver, body.getElementById(htmlNode.id));
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
      String currentUrl = driver.getCurrentUrl();
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
    return toElement(driver, maxElement);
  }
}
