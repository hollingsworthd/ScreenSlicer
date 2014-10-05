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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.request.FormLoad;
import com.screenslicer.api.request.FormQuery;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.util.Util;
import com.screenslicer.webapp.WebApp;

import edu.emory.mathcs.backport.java.util.Collections;

public class QueryForm {
  private static final double NAMED_CONTROLS_MIN_RATIO = .75;
  private static final int CHARS_TO_REMOVE = 60;
  static String delete;
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

  private static void doSubmit(RemoteWebDriver driver, String formId) throws ActionFailed {
    try {
      List<WebElement> inputs = driver.findElementById(formId).findElements(By.tagName("input"));
      List<WebElement> buttons = driver.findElementById(formId).findElements(By.tagName("button"));
      List<WebElement> possibleSubmits = new ArrayList<WebElement>();
      List<WebElement> submits = new ArrayList<WebElement>();
      possibleSubmits.addAll(inputs);
      possibleSubmits.addAll(buttons);
      for (WebElement possibleSubmit : possibleSubmits) {
        if ("submit".equalsIgnoreCase(possibleSubmit.getAttribute("type"))) {
          submits.add(possibleSubmit);
        }
      }
      boolean clicked = false;
      try {
        if (!submits.isEmpty()) {
          if (submits.size() == 1) {
            clicked = Util.click(driver, submits.get(0));
          } else {
            String formHtml = CommonUtil.strip(driver.findElementById(formId).getAttribute("outerHTML"), false);
            int minIndex = Integer.MAX_VALUE;
            WebElement firstSubmit = null;
            for (WebElement submit : submits) {
              try {
                String submitHtml = CommonUtil.strip(submit.getAttribute("outerHTML"), false);
                int submitIndex = formHtml.indexOf(submitHtml);
                if (submitIndex < minIndex) {
                  minIndex = submitIndex;
                  firstSubmit = submit;
                }
              } catch (Throwable t) {
                Log.exception(t);
              }
            }
            if (firstSubmit != null) {
              clicked = Util.click(driver, firstSubmit);
            }
          }
        }
      } catch (Throwable t) {
        Log.exception(t);
      }
      if (!clicked) {
        driver.findElementById(formId).submit();
      }
      Util.driverSleepLong();
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  public static void perform(RemoteWebDriver driver, FormQuery context) throws ActionFailed {
    try {
      Util.get(driver, context.site, true);
      Util.doClicks(driver, context.preAuthClicks, null);
      QueryCommon.doAuth(driver, context.credentials);
      Util.doClicks(driver, context.preSearchClicks, null);
      Map<String, HtmlNode> formControls = new HashMap<String, HtmlNode>();
      for (int i = 0; i < context.formSchema.length; i++) {
        formControls.put(context.formSchema[i].guid, context.formSchema[i]);
      }
      Map<String, List<String>> formData = context.formModel;
      boolean valueChanged = false;
      int count = 0;
      final int MAX_TRIES = 3;
      Element body = Util.openElement(driver, null, null, null);
      do {
        ++count;
        valueChanged = false;
        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
          try {
            HtmlNode formControl = formControls.get(entry.getKey());
            if (!CommonUtil.isEmpty(entry.getValue())) {
              if ("select".equalsIgnoreCase(formControl.tagName)) {
                if (WebApp.DEBUG) {
                  System.out.println("Query Form: select");
                }
                Select select = new Select(Util.toElement(driver, formControl, body));
                if (select.isMultiple()) {
                  select.deselectAll();
                }
                List<WebElement> selectedElements = select.getAllSelectedOptions();
                List<String> selectedStrings = new ArrayList<String>();
                for (WebElement selectedElement : selectedElements) {
                  String selectedString = selectedElement.getAttribute("value");
                  if (!CommonUtil.isEmpty(selectedString)) {
                    selectedStrings.add(selectedString);
                  }
                }
                boolean matches = true;
                for (String selectedString : selectedStrings) {
                  if (!entry.getValue().contains(selectedString)) {
                    matches = false;
                    break;
                  }
                }
                if (!matches || selectedStrings.size() != entry.getValue().size()) {
                  for (String val : entry.getValue()) {
                    valueChanged = true;
                    select.selectByValue(val);
                    Util.driverSleepVeryShort();
                  }
                }
              } else if ("input".equalsIgnoreCase(formControl.tagName)
                  && ("text".equalsIgnoreCase(formControl.type)
                  || "search".equalsIgnoreCase(formControl.type))) {
                if (WebApp.DEBUG) {
                  System.out.println("Query Form: input[text|search]");
                }
                WebElement element = Util.toElement(driver, formControl, body);
                valueChanged = QueryCommon.typeText(driver, element, entry.getValue().get(0), true, false);
              } else if ("input".equalsIgnoreCase(formControl.tagName)
                  && ("checkbox".equalsIgnoreCase(formControl.type)
                  || "radio".equalsIgnoreCase(formControl.type))) {
                if (WebApp.DEBUG) {
                  System.out.println("Query Form: input[checkbox|radio]");
                }
                WebElement element = Util.toElement(driver, formControl, body);
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                  if ("radio".equalsIgnoreCase(formControl.type)) {
                    String elementVal = element.getAttribute("value");
                    String schemaVal = formControl.value;
                    String modelVal = entry.getValue().get(0);
                    if (elementVal != null && schemaVal != null
                        && elementVal.equalsIgnoreCase(schemaVal)
                        && modelVal.equalsIgnoreCase(schemaVal)) {
                      if (!element.isSelected()) {
                        if (WebApp.DEBUG) {
                          System.out.println("Clicking radio button");
                        }
                        valueChanged = Util.click(driver, element);
                      }
                    }
                  } else if (!element.isSelected()) {
                    if (WebApp.DEBUG) {
                      System.out.println("Clicking [checkbox|radio]");
                    }
                    valueChanged = Util.click(driver, element);
                  }
                } else {
                  if (element.isSelected()) {
                    if (WebApp.DEBUG) {
                      System.out.println("Deselecting [checkbox|radio]");
                    }
                    valueChanged = true;
                    element.clear();
                    Util.driverSleepVeryShort();
                  }
                }
              }
            }
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      } while (valueChanged && count < MAX_TRIES);
      doSubmit(driver, context.formId);
      Util.doClicks(driver, context.postSearchClicks, null);
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  public static List<HtmlNode> load(RemoteWebDriver driver, FormLoad context) throws ActionFailed {
    try {
      Util.get(driver, context.site, true);
      Util.doClicks(driver, context.preAuthClicks, null);
      QueryCommon.doAuth(driver, context.credentials);
      Util.doClicks(driver, context.preSearchClicks, null);
      WebElement form = driver.findElementById(context.formId);
      Map<HtmlNode, String> controlsHtml = new HashMap<HtmlNode, String>();
      String formHtml = CommonUtil.strip(form.getAttribute("outerHTML"), false);
      List<WebElement> elements = new ArrayList<WebElement>();
      elements.addAll(form.findElements(By.tagName("input")));
      elements.addAll(form.findElements(By.tagName("button")));
      elements.addAll(form.findElements(By.tagName("textarea")));
      List<HtmlNode> controls = new ArrayList<HtmlNode>();
      for (WebElement element : elements) {
        HtmlNode control = toFormControl(element);
        if (control != null) {
          controls.add(control);
          controlsHtml.put(control, CommonUtil.strip(element.getAttribute("outerHTML"), false));
        }
      }
      elements = new ArrayList<WebElement>();
      elements.addAll(form.findElements(By.tagName("select")));
      for (WebElement element : elements) {
        HtmlNode control = toFormControl(element);
        if (control != null) {
          control.innerHtml = null;
          List<WebElement> options = element.findElements(By.tagName("option"));
          List<String> optionValues = new ArrayList<String>();
          List<String> optionLabels = new ArrayList<String>();
          for (WebElement option : options) {
            String value = option.getAttribute("value");
            String label = option.getAttribute("innerHTML");
            if (!CommonUtil.isEmpty(value) && !CommonUtil.isEmpty(label)) {
              optionValues.add(value);
              optionLabels.add(label);
            }
          }
          String multiple = element.getAttribute("multiple");
          if (!CommonUtil.isEmpty(multiple) && !"false".equalsIgnoreCase(multiple)) {
            control.multiple = "multiple";
          } else {
            multiple = element.getAttribute("data-multiple");
            if (!CommonUtil.isEmpty(multiple) && !"false".equalsIgnoreCase(multiple)) {
              control.multiple = "multiple";
            }
          }
          if ("select-multiple".equalsIgnoreCase(control.type)) {
            control.multiple = "multiple";
          }
          control.type = null;
          control.optionValues = optionValues.toArray(new String[0]);
          control.optionLabels = optionLabels.toArray(new String[0]);
          controls.add(control);
          controlsHtml.put(control, CommonUtil.strip(element.getAttribute("outerHTML"), false));
        }
      }
      loadLabels(driver, controls);
      loadGuids(controls);
      Collections.sort(controls, new ControlComparator(formHtml, controlsHtml));
      return filterControls(controls);
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  private static void loadGuids(List<HtmlNode> formControls) {
    for (HtmlNode formControl : formControls) {
      if (!CommonUtil.isEmpty(formControl.name)) {
        formControl.guidName = Crypto.fastHash(formControl.name);
      }
    }
  }

  private static class ControlComparator implements Comparator<HtmlNode> {
    private String formHtml;
    private Map<HtmlNode, String> controlsHtml;

    public ControlComparator(String formHtml, Map<HtmlNode, String> controlsHtml) {
      this.formHtml = formHtml;
      this.controlsHtml = controlsHtml;
    }

    @Override
    public int compare(HtmlNode lhs, HtmlNode rhs) {
      return Integer.compare(formHtml.indexOf(controlsHtml.get(lhs)),
          formHtml.indexOf(controlsHtml.get(rhs)));
    }
  }

  private static List<HtmlNode> filterControls(List<HtmlNode> formControls) {
    int namedControls = 0;
    int totalControls = 0;
    List<HtmlNode> filtered = new ArrayList<HtmlNode>();
    for (HtmlNode formControl : formControls) {
      if (isControlNameNeeded(formControl)) {
        ++totalControls;
        if (!CommonUtil.isEmpty(formControl.name)) {
          ++namedControls;
          filtered.add(formControl);
        }
      } else {
        filtered.add(formControl);
      }
    }
    if ((double) namedControls / (double) totalControls > NAMED_CONTROLS_MIN_RATIO) {
      return filtered;
    }
    return formControls;
  }

  private static boolean isControlNameNeeded(HtmlNode control) {
    return !control.tagName.equalsIgnoreCase("button")
        && (!control.tagName.equalsIgnoreCase("input")
            || CommonUtil.isEmpty(control.type)
            || !control.type.equalsIgnoreCase("button")
            || !control.type.equalsIgnoreCase("submit"));
  }

  private static void loadLabels(RemoteWebDriver driver, List<HtmlNode> controls) throws ActionFailed {
    try {
      List<WebElement> labels = driver.findElementsByTagName("label");
      Map<String, String> labelMap = new HashMap<String, String>();
      for (WebElement label : labels) {
        String labelFor = label.getAttribute("for");
        String labelText = label.getText();
        if (!CommonUtil.isEmpty(labelFor) && !CommonUtil.isEmpty(labelText)) {
          labelMap.put(labelFor.trim(), labelText.trim());
        }
      }
      for (HtmlNode control : controls) {
        if (!CommonUtil.isEmpty(control.id)) {
          control.label = labelMap.get(control.id);
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
      throw new ActionFailed(t);
    }
  }

  private static HtmlNode toFormControl(WebElement element) {
    if (!element.isDisplayed()) {
      return null;
    }
    HtmlNode control = new HtmlNode();
    control.tagName = element.getTagName();
    String attr = element.getAttribute("name");
    control.name = CommonUtil.isEmpty(attr) ? null : attr;
    attr = element.getAttribute("title");
    control.title = CommonUtil.isEmpty(attr) ? null : attr;
    attr = element.getAttribute("id");
    control.id = CommonUtil.isEmpty(attr) ? null : attr;
    attr = element.getAttribute("type");
    control.type = CommonUtil.isEmpty(attr) ? null : attr;
    attr = element.getAttribute("value");
    control.value = CommonUtil.isEmpty(attr) ? null : attr;
    attr = element.getAttribute("innerHTML");
    control.innerHtml = CommonUtil.isEmpty(attr) ? null : attr;
    if ("hidden".equalsIgnoreCase(control.type)) {
      return null;
    }
    return control;
  }
}
