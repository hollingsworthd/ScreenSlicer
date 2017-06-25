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
