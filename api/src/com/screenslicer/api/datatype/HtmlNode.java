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
package com.screenslicer.api.datatype;

import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Random;

public final class HtmlNode {
  public static final HtmlNode instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<HtmlNode> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final HtmlNode instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(HtmlNode.class, args);
  }

  public static final List<HtmlNode> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(HtmlNode.class, args);
  }

  public static final List<HtmlNode> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(HtmlNode.class, args);
  }

  public static final String toJson(HtmlNode obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<HtmlNode>() {}.getType());
  }

  public static final String toJson(HtmlNode[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<HtmlNode[]>() {}.getType());
  }

  public static final String toJson(List<HtmlNode> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<HtmlNode>>() {}.getType());
  }

  /**
   * Whether the HtmlNode parameters are hints (fuzzy == true)
   * or whether they specify a precise values (fuzzy == false).
   * Defaults to false.
   * Regardless of this value, HREFs will be canonicalized to avoid
   * insignificant differences in domain/protocol/path.
   */
  public boolean fuzzy;
  /**
   * Whether the HtmlNodes parameters must all be matched (any == false)
   * or whether any single one must match (any == true).
   * Defaults to true;
   */
  public boolean any = true;

  /**
   * Whether actions on this node should be followed by a long delay.
   * Useful for AJAX apps, such as with a search button that does an asynchonous
   * post.
   */
  public boolean longRequest = false;

  /**
   * Specifies a URL to HTTP GET directly, instead of a DOM element.
   */
  public String httpGet;
  /**
   * HTML tag name (e.g., select, button, form, a)
   */
  public String tagName;
  /**
   * HTML element ID attribute
   */
  public String id;
  /**
   * HTML element name attribute
   */
  public String name;
  /**
   * HTML label value
   */
  public String label;

  /**
   * HTML element type attribute (e.g., "text" for &lt;input type="text"&gt;)
   */
  public String type;
  /**
   * HTML element value attribute
   */
  public String value;
  /**
   * HTML element title attribute
   */
  public String title;
  /**
   * HTML element alt attribute
   */
  public String alt;
  /**
   * HTML element HREF attribute (e.g., HREFs on an anchor)
   */
  public String href;
  /**
   * CSS class names listed in the class attribute of an HTML element
   */
  public String[] classes;

  /**
   * Text contained in an HTML node
   */
  public String innerText;
  /**
   * HTML contained in an HTML node
   */
  public String innerHtml;

  /**
   * Whether mutli-select is enabled (only valid for checkboxes and selects)
   */
  public String multiple;
  /**
   * Values of the element
   */
  public String[] optionValues;
  /**
   * Labels of the element
   */
  public String[] optionLabels;

  /**
   * GUID (a random string) for this node
   */
  public String guid = Random.next();
  /**
   * GUID which can be shared (like how multiple elements can have the same
   * name)
   */
  public String guidName = guid;

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("(HtmlNode) ");
    builder.append(toStringHelper("tagName", tagName));
    builder.append(toStringHelper("id", id));
    builder.append(toStringHelper("name", name));
    builder.append(toStringHelper("label", label));
    builder.append(toStringHelper("type", type));
    builder.append(toStringHelper("value", value));
    builder.append(toStringHelper("title", title));
    builder.append(toStringHelper("alt", alt));
    builder.append(toStringHelper("href", href));
    builder.append(toStringHelper("classes", CommonUtil.toString(classes, ",")));
    builder.append(toStringHelper("innerText", innerText));
    builder.append(toStringHelper("innerHtml", innerHtml));
    builder.append(toStringHelper("multiple", multiple));
    builder.append(toStringHelper("optionLabels", CommonUtil.toString(optionLabels, ",")));
    builder.append(toStringHelper("optionValues", CommonUtil.toString(optionValues, ",")));
    builder.append(toStringHelper("guid", guid));
    builder.append(toStringHelper("guidName", guidName));
    return builder.toString();
  }

  private static String toStringHelper(String name, String value) {
    if (!CommonUtil.isEmpty(value)) {
      return name + "=" + value + "|";
    }
    return "";
  }
}
