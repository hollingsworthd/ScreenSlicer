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
package com.screenslicer.api.datatype;

import java.util.List;
import java.util.Map;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Crypto;

public final class HtmlNode {
  public static final HtmlNode instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(HtmlNode.class, args);
  }

  public static final List<HtmlNode> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(HtmlNode.class, args);
  }

  public boolean longRequest = false;

  public String tagName;
  public String id;
  public String name;
  public String label;

  public String type;
  public String value;
  public String title;
  public String href;
  public String[] classes;

  public String innerText;
  public String innerHtml;

  public String multiple;
  public String[] optionValues;
  public String[] optionLabels;

  public String guid = Crypto.random();
  public String guidName = guid;
}
