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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.BrowserDriver;
import org.openqa.selenium.remote.BrowserDriver.Fatal;
import org.openqa.selenium.remote.BrowserDriver.Retry;

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
  private static final int CHARS_TO_REMOVE = 60;
  private static final Pattern searchControl = Pattern.compile(
      "(?:[-._\"]|^|\\b|\\s|/|#)(?:search|magnifying|magnify|搜索|بحث|Recherche|Chercher|Suche|खोज|Cerca|検索|Поиск|Buscar)(?:[-._\"]|$|\\b|\\s|/|#)",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern search = Pattern.compile(
      "search|find|query|keyword|locate|google|results|(?:(?:[-._\"]|^|\\b|\\s|/|#)q(?:[-._\"]|$|\\b|\\s|/|#))|搜索|بحث|Recherche|Chercher|Suche|खोज|Cerca|検索|Поиск|Buscar",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern nonSearch = Pattern.compile(
      "username|e-?mail|user|log\\s?in|uname|city|state|zip|location", //TODO translate
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  private static String delete;
  static {
    String key = Keys.BACK_SPACE.toString();
    delete = key;
    for (int i = 1; i < CHARS_TO_REMOVE; i++) {
      delete = delete + key;
    }
    key = Keys.DELETE.toString();
    delete = delete + key;
    for (int i = 1; i < CHARS_TO_REMOVE; i++) {
      delete = delete + key;
    }
  }

  private static List<WebElement> findSearchBox(
      BrowserDriver driver, boolean strict) throws ActionFailed {
    try {
      List<WebElement> searchBoxes = new ArrayList<WebElement>();
      List<WebElement> allInputs = driver.findElementsByTagName("input");
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
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static void hoverClick(BrowserDriver driver, WebElement element, boolean cleanupWindows) throws ActionFailed {
    try {
      String oldHandle = driver.getWindowHandle();
      Actions action = driver.actions();
      BrowserUtil.click(driver, element, false);
      action.moveByOffset(-MOUSE_MOVE_OFFSET, -MOUSE_MOVE_OFFSET).perform();
      action.moveToElement(element).perform();
      action.moveByOffset(2, 2).perform();
      BrowserUtil.driverSleepShort();
      BrowserUtil.handleNewWindows(driver, oldHandle, cleanupWindows);
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static List<WebElement> navigateToSearch(
      BrowserDriver driver, String tagName, String type, boolean strict, boolean cleanupWindows) throws ActionFailed {
    try {
      List<WebElement> tags = driver.findElementsByTagName(tagName);
      boolean success = false;
      for (WebElement tag : tags) {
        try {
          if (type == null
              || type.equalsIgnoreCase(tag.getAttribute("type"))) {
            String info = new String(tag.getAttribute("href")
                + " " + tag.getText());
            if (searchControl.matcher(info).find()) {
              hoverClick(driver, tag, cleanupWindows);
              success = true;
              break;
            }
          }
        } catch (Retry r) {
          throw r;
        } catch (Fatal f) {
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
                hoverClick(driver, tag, cleanupWindows);
                success = true;
                break;
              }
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
      if (success) {
        return findSearchBox(driver, strict);
      }
      return new ArrayList<WebElement>();
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  private static String doSearch(BrowserDriver driver, List<WebElement> searchBoxes,
      String searchQuery, HtmlNode submitClick, boolean cleanupWindows) throws ActionFailed {
    try {
      for (WebElement element : searchBoxes) {
        try {
          BrowserUtil.click(driver, element, false);
          element.clear();
          BrowserUtil.driverSleepVeryShort();
          if (!CommonUtil.isEmpty(element.getAttribute("value"))) {
            element.sendKeys(delete);
            BrowserUtil.driverSleepVeryShort();
          }
          element.sendKeys(searchQuery);
          BrowserUtil.driverSleepVeryShort();
          String beforeSource = driver.getPageSource();
          String beforeTitle = driver.getTitle();
          String beforeUrl = driver.getCurrentUrl();
          String windowHandle = driver.getWindowHandle();
          if (submitClick == null) {
            element.sendKeys("\n");
          } else {
            BrowserUtil.click(driver, BrowserUtil.toElement(driver, submitClick, null), false);
          }
          BrowserUtil.driverSleepLong();
          BrowserUtil.handleNewWindows(driver, windowHandle, cleanupWindows);
          String afterSource = driver.getPageSource();
          String afterTitle = driver.getTitle();
          String afterUrl = driver.getCurrentUrl();
          if (!beforeTitle.equals(afterTitle)
              || !beforeUrl.equals(afterUrl)
              || Math.abs(beforeSource.length() - afterSource.length()) > MIN_SOURCE_DIFF) {
            handleIframe(driver, cleanupWindows);
            return driver.getPageSource();
          }
        } catch (Retry r) {
          throw r;
        } catch (Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    throw new ActionFailed();
  }

  private static void handleIframe(BrowserDriver driver, boolean cleanupWindows) throws ActionFailed {
    List<WebElement> iframes = null;
    try {
      iframes = driver.findElementsByTagName("iframe");
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
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
                origHandle = driver.getWindowHandle();
                origUrl = driver.getCurrentUrl();
                newHandle = BrowserUtil.newWindow(driver, cleanupWindows);
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
                throw f;
              } catch (Throwable t) {
                throw new ActionFailed(t);
              }
              boolean undo = false;
              try {
                BrowserUtil.get(driver, src, true, cleanupWindows);
                driver.executeScript("document.getElementsByTagName('html')[0].style.overflow='scroll';");
                BrowserUtil.driverSleepShort();
                if (driver.findElementByTagName("body").getText().length() < MIN_SOURCE_DIFF) {
                  undo = true;
                }
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
                throw f;
              } catch (Throwable t) {
                Log.exception(t);
                undo = true;
              }
              try {
                if (undo) {
                  if (origHandle.equals(newHandle)) {
                    if (!driver.getCurrentUrl().equals(origUrl)) {
                      try {
                        driver.navigate().back();
                      } catch (Retry r) {
                        throw r;
                      } catch (Fatal f) {
                        throw f;
                      } catch (Throwable t) {
                        Log.exception(t);
                      }
                    }
                    if (!driver.getCurrentUrl().equals(origUrl)) {
                      driver.get(origUrl);
                    }
                  } else {
                    BrowserUtil.handleNewWindows(driver, origHandle, cleanupWindows);
                  }
                } else {
                  BrowserUtil.handleNewWindows(driver, newHandle, cleanupWindows);
                  break;
                }
              } catch (Retry r) {
                throw r;
              } catch (Fatal f) {
                throw f;
              } catch (Throwable t) {
                throw new ActionFailed(t);
              }
            }
          }
        } catch (Retry r) {
          throw r;
        } catch (Fatal f) {
          throw f;
        } catch (Throwable t) {
          Log.exception(t);
          continue;
        }
      }
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }

  public static void perform(BrowserDriver driver, KeywordQuery context, boolean cleanupWindows) throws ActionFailed {
    try {
      if (!CommonUtil.isEmpty(context.site)) {
        BrowserUtil.get(driver, context.site, true, cleanupWindows);
      }
      BrowserUtil.doClicks(driver, context.preAuthClicks, null, false);
      QueryCommon.doAuth(driver, context.credentials);
      BrowserUtil.doClicks(driver, context.preSearchClicks, null, false);
      if (!CommonUtil.isEmpty(context.keywords)) {
        List<WebElement> searchBoxes = findSearchBox(driver, true);
        String searchResult = doSearch(driver, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
        String[] fallbackNames =
            new String[] { "button", "input", "input", "div", "label", "span", "li", "ul", "a" };
        String[] fallbackTypes =
            new String[] { null, "button", "submit", null, null, null, null, null, null };
        for (int i = 0; i < fallbackNames.length && searchResult == null; i++) {
          searchBoxes = navigateToSearch(driver,
              fallbackNames[i], fallbackTypes[i], true, cleanupWindows);
          searchResult = doSearch(driver, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
        }
        if (searchResult == null) {
          searchBoxes = findSearchBox(driver, false);
          searchResult = doSearch(driver, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
        }
        if (searchResult == null) {
          for (int i = 0; i < fallbackNames.length && searchResult == null; i++) {
            searchBoxes = navigateToSearch(driver,
                fallbackNames[i], fallbackTypes[i], false, cleanupWindows);
            searchResult = doSearch(driver, searchBoxes, context.keywords, context.searchSubmitClick, cleanupWindows);
          }
        }
      }
      BrowserUtil.doClicks(driver, context.postSearchClicks, null, false);
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
  }
}
