/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
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
import org.openqa.selenium.remote.RemoteWebDriver;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.util.Util;

public class Query {
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
      RemoteWebDriver driver, boolean strict) throws ActionFailed {
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
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  private static void hoverClick(RemoteWebDriver driver, WebElement element) throws ActionFailed {
    try {
      String oldHandle = driver.getWindowHandle();
      Actions action = new Actions(driver);
      Util.click(driver, element);
      action.moveByOffset(-MOUSE_MOVE_OFFSET, -MOUSE_MOVE_OFFSET).perform();
      action.moveToElement(element).perform();
      action.moveByOffset(2, 2).perform();
      Util.driverSleepShort();
      Util.cleanUpNewWindows(driver, oldHandle);
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  private static List<WebElement> navigateToSearch(
      RemoteWebDriver driver, String tagName, String type, boolean strict) throws ActionFailed {
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
              hoverClick(driver, tag);
              success = true;
              break;
            }
          }
        } catch (Exception e) {
          Log.exception(e);
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
                hoverClick(driver, tag);
                success = true;
                break;
              }
            }
          } catch (Exception e) {
            Log.exception(e);
          }
        }
      }
      if (success) {
        return findSearchBox(driver, strict);
      }
      return new ArrayList<WebElement>();
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  private static String doSearch(RemoteWebDriver driver, List<WebElement> searchBoxes,
      String searchQuery) throws ActionFailed {
    try {
      for (WebElement element : searchBoxes) {
        try {
          Util.click(driver, element);
          element.clear();
          Util.driverSleepVeryShort();
          if (!CommonUtil.isEmpty(element.getAttribute("value"))) {
            element.sendKeys(delete);
            Util.driverSleepVeryShort();
          }
          element.sendKeys(searchQuery);
          Util.driverSleepVeryShort();
          String beforeSource = driver.getPageSource();
          String beforeTitle = driver.getTitle();
          String beforeUrl = driver.getCurrentUrl();
          String windowHandle = driver.getWindowHandle();
          element.sendKeys("\n");
          Util.driverSleepLong();
          Util.cleanUpNewWindows(driver, windowHandle);
          String afterSource = driver.getPageSource();
          String afterTitle = driver.getTitle();
          String afterUrl = driver.getCurrentUrl();
          if (!beforeTitle.equals(afterTitle)
              || !beforeUrl.equals(afterUrl)
              || Math.abs(beforeSource.length() - afterSource.length()) > MIN_SOURCE_DIFF) {
            handleIframe(driver);
            return driver.getPageSource();
          }
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
    throw new ActionFailed();
  }

  private static void handleIframe(RemoteWebDriver driver) throws ActionFailed {
    List<WebElement> iframes = null;
    try {
      iframes = driver.findElementsByTagName("iframe");
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
                newHandle = Util.newWindow(driver);
              } catch (Throwable t) {
                Log.exception(t);
                throw new ActionFailed(t);
              }
              boolean undo = false;
              try {
                Util.get(driver, src, true);
                driver.executeScript("document.getElementsByTagName('html')[0].style.overflow='scroll';");
                Util.driverSleepShort();
                if (driver.findElementByTagName("body").getText().length() < MIN_SOURCE_DIFF) {
                  undo = true;
                }
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
                      } catch (Throwable t) {
                        Log.exception(t);
                      }
                    }
                    if (!driver.getCurrentUrl().equals(origUrl)) {
                      driver.get(origUrl);
                    }
                  } else {
                    Util.cleanUpNewWindows(driver, origHandle);
                  }
                } else {
                  Util.cleanUpNewWindows(driver, newHandle);
                  break;
                }
              } catch (Throwable t) {
                Log.exception(t);
                throw new ActionFailed(t);
              }
            }
          }
        } catch (Throwable t) {
          Log.exception(t);
          continue;
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  public static void perform(RemoteWebDriver driver,
      String url, String searchQuery) throws ActionFailed {
    try {
      Util.get(driver, url, true);
      List<WebElement> searchBoxes = findSearchBox(driver, true);
      String searchResult = doSearch(driver, searchBoxes, searchQuery);
      String[] fallbackNames =
          new String[] { "button", "input", "input", "div", "label", "span", "li", "ul", "a" };
      String[] fallbackTypes =
          new String[] { null, "button", "submit", null, null, null, null, null, null };
      for (int i = 0; i < fallbackNames.length && searchResult == null; i++) {
        searchBoxes = navigateToSearch(driver,
            fallbackNames[i], fallbackTypes[i], true);
        searchResult = doSearch(driver, searchBoxes, searchQuery);
      }
      if (searchResult == null) {
        searchBoxes = findSearchBox(driver, false);
        searchResult = doSearch(driver, searchBoxes, searchQuery);
      }
      if (searchResult == null) {
        for (int i = 0; i < fallbackNames.length && searchResult == null; i++) {
          searchBoxes = navigateToSearch(driver,
              fallbackNames[i], fallbackTypes[i], false);
          searchResult = doSearch(driver, searchBoxes, searchQuery);
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }
}
