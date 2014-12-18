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

import java.util.regex.Pattern;

import com.screenslicer.common.CommonUtil;

public class HtmlUtil {
  private static Pattern attributes = Pattern.compile("(?<=<\\w{1,15}\\s)[^>]+(?=>)", Pattern.UNICODE_CHARACTER_CLASS);

  public static String stripAttributes(String str, boolean stripAllSpaces) {
    String ret = CommonUtil.strip(str, false);
    ret = attributes.matcher(ret).replaceAll("");
    if (stripAllSpaces) {
      return ret.replace(" ", "");
    }
    return ret;
  }

  public static int trimmedLen(String str) {
    if (str.isEmpty()) {
      return 0;
    }
    int count = 0;
    boolean prevWhitespace = false;
    str = str.replaceAll("&nbsp;", " ").replaceAll("&amp;nbsp;", " ").trim();
    for (int i = 0; i < str.length(); i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        ++count;
        prevWhitespace = false;
      } else if (!prevWhitespace) {
        ++count;
        prevWhitespace = true;
      }
    }
    return count;
  }
}
