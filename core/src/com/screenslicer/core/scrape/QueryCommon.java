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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.BrowserDriver;
import org.openqa.selenium.remote.BrowserDriver.Fatal;
import org.openqa.selenium.remote.BrowserDriver.Retry;

import com.screenslicer.api.datatype.Credentials;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.util.BrowserUtil;

public class QueryCommon {

  public static boolean doAuth(BrowserDriver driver, Credentials credentials) throws ActionFailed {
    if (credentials == null
        || CommonUtil.isEmpty(credentials.username) || CommonUtil.isEmpty(credentials.password)) {
      return false;
    }
    try {
      List<WebElement> inputs = driver.findElementsByTagName("input");
      String html = CommonUtil.strip(driver.findElementByTagName("body").getAttribute("outerHTML"), true);
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
        QueryCommon.typeText(driver, closestLogin.username, credentials.username, true, false);
        QueryCommon.typeText(driver, closestLogin.password, credentials.password, false, true);
        return true;
      }
    } catch (Retry r) {
      throw r;
    } catch (Fatal f) {
      throw f;
    } catch (Throwable t) {
      throw new ActionFailed(t);
    }
    throw new ActionFailed("Could not sign in");
  }

  public static boolean typeText(BrowserDriver driver, WebElement element, String text, boolean validate, boolean newline) {
    String elementVal = null;
    if (validate) {
      elementVal = element.getAttribute("value");
    }
    if (!validate || !text.equalsIgnoreCase(elementVal)) {
      BrowserUtil.click(driver, element, false);
      if (validate) {
        element.clear();
        BrowserUtil.driverSleepVeryShort();
      }
      if (!validate || !CommonUtil.isEmpty(element.getAttribute("value"))) {
        element.sendKeys(QueryForm.delete);
        BrowserUtil.driverSleepVeryShort();
      }
      element.sendKeys(text);
      driver.getKeyboard().sendKeys("\t");
      BrowserUtil.driverSleepVeryShort();
      if (newline) {
        element.sendKeys("\n");
        BrowserUtil.driverSleepLong();
      }
      return true;
    }
    return false;
  }

}
