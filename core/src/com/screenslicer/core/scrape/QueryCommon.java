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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;

import com.machinepublishers.browser.Browser;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.screenslicer.api.datatype.Credentials;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.util.BrowserUtil;

public class QueryCommon {

  public static boolean doAuth(Browser browser, Credentials credentials) throws ActionFailed {
    if (credentials == null
        || CommonUtil.isEmpty(credentials.username) || CommonUtil.isEmpty(credentials.password)) {
      return false;
    }
    try {
      List<WebElement> inputs = browser.findElementsByTagName("input");
      String html = CommonUtil.strip(browser.findElementByTagName("body").getAttribute("outerHTML"), true);
      List<WebElement> usernames = new ArrayList<WebElement>();
      List<WebElement> passwords = new ArrayList<WebElement>();
      List<String> usernamesHtml = new ArrayList<String>();
      List<String> passwordsHtml = new ArrayList<String>();
      for (WebElement input : inputs) {
        String type = input.getAttribute("type");
        if ("text".equalsIgnoreCase(type)) {
          usernames.add(input);
          String controlHtml = input.getAttribute("outerHTML");
          controlHtml = CommonUtil.strip(controlHtml, true);
          usernamesHtml.add(controlHtml);
        } else if ("password".equalsIgnoreCase(type)) {
          passwords.add(input);
          String controlHtml = input.getAttribute("outerHTML");
          controlHtml = CommonUtil.strip(controlHtml, true);
          passwordsHtml.add(controlHtml);
        }
      }
      class Login {
        final WebElement username;
        final WebElement password;
        final int index;

        Login(WebElement username, WebElement password, int index) {
          this.username = username;
          this.password = password;
          this.index = index;
        }
      };
      List<Login> logins = new ArrayList<Login>();
      for (int curPassword = 0; curPassword < passwords.size(); curPassword++) {
        int passwordIndex = html.indexOf(passwordsHtml.get(curPassword));
        int minDist = Integer.MAX_VALUE;
        int indexOfMin = -1;
        WebElement minUsername = null;
        WebElement minPassword = passwords.get(curPassword);
        for (int curUsername = 0; curUsername < usernames.size(); curUsername++) {
          int usernameIndex = html.indexOf(usernamesHtml.get(curUsername));
          if (usernameIndex < passwordIndex
              && passwordIndex - usernameIndex < minDist
              && usernameIndex > -1 && passwordIndex > -1) {
            minDist = passwordIndex - usernameIndex;
            minUsername = usernames.get(curUsername);
            indexOfMin = (usernameIndex + passwordIndex) / 2;
          }
        }
        logins.add(new Login(minUsername, minPassword, indexOfMin));
      }
      if (!logins.isEmpty()) {
        Login closestLogin = logins.get(0);
        if (logins.size() > 1) {
          //TODO translate
          Pattern hints = Pattern.compile(
              "(?:log(?:ged)?\\s?-?in)|(?:sign(?:ed)?\\s?\\-?in)|(?:remember\\s?me)|(?:tabindex\\s?=\\s?[^0-9]?[12][^0-9])",
              Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
          Matcher matcher = hints.matcher(html);
          int closest = Integer.MAX_VALUE;
          while (matcher.find()) {
            int start = matcher.start();
            for (Login login : logins) {
              int dist = Math.abs(login.index - start);
              if (dist < closest) {
                closest = dist;
                closestLogin = login;
              }
            }
          }
        }
        QueryCommon.typeText(browser, closestLogin.username, credentials.username, true, false);
        QueryCommon.typeText(browser, closestLogin.password, credentials.password, false, true);
        return true;
      }
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
    throw new ActionFailed("Could not sign in");
  }

  public static boolean typeText(Browser browser, WebElement element, String text, boolean validate, boolean newline) {
    String elementVal = null;
    if (validate) {
      elementVal = element.getAttribute("value");
    }
    if (!validate || !text.equalsIgnoreCase(elementVal)) {
      BrowserUtil.click(browser, element, false);
      if (validate) {
        element.clear();
      }
      if (!validate || !CommonUtil.isEmpty(element.getAttribute("value"))) {
        element.sendKeys(JBrowserDriver.KEYBOARD_DELETE);
      }
      element.sendKeys(text);
      browser.getKeyboard().sendKeys("\t");
      if (newline) {
        element.sendKeys("\n");
        browser.getStatusCode();
      }
      return true;
    }
    return false;
  }

}
