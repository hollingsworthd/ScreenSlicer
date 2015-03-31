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
package com.screenslicer.core.scrape;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.machinepublishers.browser.Browser;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.util.BrowserUtil;

public class QueryKeyword {
  private static final int MIN_IFRAME_AREA = 40000;
  private static final int MIN_SOURCE_DIFF = 500;
  private static final int MOUSE_MOVE_OFFSET = 500;
  private static final Pattern searchControl = Pattern.compile(
      "(?:[-._\"]|^|\\b|\\s|/|#)(?:search|magnifying|magnify|搜索|بحث|Recherche|Chercher|Suche|खोज|Cerca|検索|Поиск|Buscar)(?:[-._\"]|$|\\b|\\s|/|#)",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern search = Pattern.compile(
      "search|find|query|keyword|locate|google|results|(?:(?:[-._\"]|^|\\b|\\s|/|#)q(?:[-._\"]|$|\\b|\\s|/|#))|搜索|بحث|Recherche|Chercher|Suche|खोज|Cerca|検索|Поиск|Buscar",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern nonSearch = Pattern.compile(
      "username|e-?mail|user|log\\s?in|uname|city|state|zip|location", //TODO translate
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

  private static List<WebElement> findSearchBox(
      Browser browser, boolean strict) throws ActionFailed {
    try {
      List<WebElement> searchBoxes = new ArrayList<WebElement>();
      List<WebElement> allInputs = browser.findElementsByTagName("input");
      for (WebElement searchBox : allInputs) {
        if (searchBox.getAttribute("type").equalsIgnoreCase("text")
            || searchBox.getAttribute("type").equalsIgnoreCase("search")) {
          searchBoxes.add(searchBox);
        }
      }
      List<WebElement> prioritySearchBoxes = new ArrayList<WebElement>();
      List<WebElement> forbiddenSearchBoxes = new ArrayList<WebElement>();
      for (WebElement searchBox : searchBoxes) {
        String info = new String(searchBox.getAttribute("name")
            + " " + searchBox.getAttribute("title")
            + " " + searchBox.getAttribute("value")
            + " " + searchBox.getAttribute("class")
            + " " + searchBox.getAttribute("placeholder")
            + " " + searchBox.getAttribute("id"));
        if (strict && nonSearch.matcher(info).find()) {
          forbiddenSearchBoxes.add(searchBox);
        } else if (search.matcher(info).find()) {
          prioritySearchBoxes.add(0, searchBox);
        }
      }
      for (WebElement searchBox : forbiddenSearchBoxes) {
        searchBoxes.remove(searchBox);
      }
      for (WebElement searchBox : prioritySearchBoxes) {
        searchBoxes.remove(searchBox);
        searchBoxes.add(0, searchBox);
      }
      if (!strict && searchBoxes.isEmpty() && allInputs.size() == 1) {
        searchBoxes.add(allInputs.get(0));
      } else if (!strict && searchBoxes.isEmpty() && prioritySearchBoxes.size() == 1) {
        searchBoxes.add(prioritySearchBoxes.get(0));
      }
      return searchBoxes;
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static void hoverClick(Browser browser, WebElement element, boolean cleanupWindows) throws ActionFailed {
    try {
      String oldHandle = browser.getWindowHandle();
      Actions action = new Actions(browser);
      BrowserUtil.click(browser, element, false);
      action.moveByOffset(-MOUSE_MOVE_OFFSET, -MOUSE_MOVE_OFFSET).perform();
      action.moveToElement(element).perform();
      action.moveByOffset(2, 2).perform();
      browser.getStatusCode();
      BrowserUtil.handleNewWindows(browser, oldHandle, cleanupWindows);
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static List<WebElement> navigateToSearch(
      Browser browser, String tagName, String type, boolean strict, boolean cleanupWindows) throws ActionFailed {
    try {
      List<WebElement> tags = browser.findElementsByTagName(tagName);
      boolean success = false;
      for (WebElement tag : tags) {
        try {
          if (type == null
              || type.equalsIgnoreCase(tag.getAttribute("type"))) {
            String info = new String(tag.getAttribute("href")
                + " " + tag.getText());
            if (searchControl.matcher(info).find()) {
              hoverClick(browser, tag, cleanupWindows);
              success = true;
              break;
            }
          }
        } catch (Browser.Retry r) {
          throw r;
        } catch (Browser.Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
      if (!success) {
        for (WebElement tag : tags) {
          try {
            if (type == null
                || type.equalsIgnoreCase(tag.getAttribute("type"))) {
              String extendedInfo = new String(tag.getAttribute("name")
                  + " " + tag.getAttribute("title")
                  + " " + tag.getAttribute("class")
                  + " " + tag.getAttribute("id"));
              if (searchControl.matcher(extendedInfo).find()) {
                hoverClick(browser, tag, cleanupWindows);
                success = true;
                break;
              }
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
      if (success) {
        return findSearchBox(browser, strict);
      }
      return new ArrayList<WebElement>();
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static String doSearch(Browser browser, List<WebElement> searchBoxes,
      String searchQuery, HtmlNode submitClick, boolean cleanupWindows) throws ActionFailed {
    try {
      for (WebElement element : searchBoxes) {
        try {
          BrowserUtil.click(browser, element, false);
          element.clear();
          if (!CommonUtil.isEmpty(element.getAttribute("value"))) {
            element.sendKeys(JBrowserDriver.KEYBOARD_DELETE);
          }
          element.sendKeys(searchQuery);
          String beforeSource = browser.getPageSource();
          String beforeTitle = browser.getTitle();
          String beforeUrl = browser.getCurrentUrl();
          String windowHandle = browser.getWindowHandle();
          if (submitClick == null) {
            element.sendKeys("\n");
          } else {
            BrowserUtil.click(browser, BrowserUtil.toElement(browser, submitClick, null), false);
          }
          browser.getStatusCode();
          BrowserUtil.handleNewWindows(browser, windowHandle, cleanupWindows);
          String afterSource = browser.getPageSource();
          String afterTitle = browser.getTitle();
          String afterUrl = browser.getCurrentUrl();
          if (!beforeTitle.equals(afterTitle)
              || !beforeUrl.equals(afterUrl)
              || Math.abs(beforeSource.length() - afterSource.length()) > MIN_SOURCE_DIFF) {
            handleIframe(browser, cleanupWindows);
            return browser.getPageSource();
          }
        } catch (Browser.Retry r) {
          throw r;
        } catch (Browser.Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    throw new ActionFailed();
  }

  private static void handleIframe(Browser browser, boolean cleanupWindows) throws ActionFailed {
    List<WebElement> iframes = null;
    try {
      iframes = browser.findElementsByTagName("iframe");
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
    try {
      for (WebElement iframe : iframes) {
        try {
          Dimension dim = iframe.getSize();
          if (iframe.isDisplayed() && (dim.getHeight() * dim.getWidth()) > MIN_IFRAME_AREA) {
            String src = iframe.getAttribute("src");
            if (!CommonUtil.isEmpty(src) && src.indexOf("://") > -1 && src.indexOf("?") > -1) {
              String origHandle = null;
              String origUrl = null;
              String newHandle = null;
              try {
                origHandle = browser.getWindowHandle();
                origUrl = browser.getCurrentUrl();
                newHandle = BrowserUtil.newWindow(browser, cleanupWindows);
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (Throwable t) {
                throw new ActionFailed(t);
              }
              boolean undo = false;
              try {
                BrowserUtil.get(browser, src, true, cleanupWindows);
                browser.executeScript("document.getElementsByTagName('html')[0].style.overflow='scroll';");
                BrowserUtil.browserSleepShort();
                if (browser.findElementByTagName("body").getText().length() < MIN_SOURCE_DIFF) {
                  undo = true;
                }
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (Throwable t) {
                Log.exception(t);
                undo = true;
              }
              try {
                if (undo) {
                  if (origHandle.equals(newHandle)) {
                    if (!browser.getCurrentUrl().equals(origUrl)) {
                      try {
                        browser.navigate().back();
                      } catch (Browser.Retry r) {
                        throw r;
                      } catch (Browser.Fatal f) {
                        throw f;
                      } catch (Throwable t) {
                        Log.exception(t);
                      }
                    }
                    if (!browser.getCurrentUrl().equals(origUrl)) {
                      browser.get(origUrl);
                    }
                  } else {
                    BrowserUtil.handleNewWindows(browser, origHandle, cleanupWindows);
                  }
                } else {
                  BrowserUtil.handleNewWindows(browser, newHandle, cleanupWindows);
                  break;
                }
              } catch (Browser.Retry r) {
                throw r;
              } catch (Browser.Fatal f) {
                throw f;
              } catch (Throwable t) {
                throw new ActionFailed(t);
              }
            }
          }
        } catch (Browser.Retry r) {
          throw r;
        } catch (Browser.Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
          continue;
        }
      }
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static void perform(Browser browser, KeywordQuery context, boolean cleanupWindows) throws ActionFailed {
    try {
      if (!CommonUtil.isEmpty(context.site)) {
        BrowserUtil.get(browser, context.site, true, cleanupWindows);
      }
      BrowserUtil.doClicks(browser, context.preAuthClicks, null, false);
      QueryCommon.doAuth(browser, context.credentials);
      BrowserUtil.doClicks(browser, context.preSearchClicks, null, false);
      if (!CommonUtil.isEmpty(context.keywords)) {
        List<WebElement> searchBoxes = findSearchBox(browser, true);
        String searchResult = doSearch(browser, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
        String[] fallbackNames =
            new String[] { "button", "input", "input", "div", "label", "span", "li", "ul", "a" };
        String[] fallbackTypes =
            new String[] { null, "button", "submit", null, null, null, null, null, null };
        for (int i = 0; i < fallbackNames.length && searchResult == null; i++) {
          searchBoxes = navigateToSearch(browser,
              fallbackNames[i], fallbackTypes[i], true, cleanupWindows);
          searchResult = doSearch(browser, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
        }
        if (searchResult == null) {
          searchBoxes = findSearchBox(browser, false);
          searchResult = doSearch(browser, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
        }
        if (searchResult == null) {
          for (int i = 0; i < fallbackNames.length && searchResult == null; i++) {
            searchBoxes = navigateToSearch(browser,
                fallbackNames[i], fallbackTypes[i], false, cleanupWindows);
            searchResult = doSearch(browser, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
          }
        }
      }
      BrowserUtil.doClicks(browser, context.postSearchClicks, null, false);
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }
}
