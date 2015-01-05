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
package com.screenslicer.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class StringUtil {
  private static final Map<String, Integer> distCache = new HashMap<String, Integer>();
  private static final int MAX_DIST_CACHE = 4000;

  public static int diff(Collection<String> collectionA, Collection<String> collectionB) {
    Collection<String> composite = new HashSet<String>();
    composite.addAll(collectionA);
    composite.addAll(collectionB);
    int diff = 0;
    for (String string : composite) {
      if (!collectionA.contains(string) || !collectionB.contains(string)) {
        diff++;
      }
    }
    return diff;
  }

  public static boolean contains(List<String> list, List<List<String>> listOfLists) {
    for (List<String> cur : listOfLists) {
      if (isSame(list, cur)) {
        return true;
      }
    }
    return false;
  }

  public static int dist(String str1, String str2) {
    if (distCache.size() > MAX_DIST_CACHE) {
      distCache.clear();
    }
    String cacheKey = str1 + "<<>>" + str2;
    if (distCache.containsKey(cacheKey)) {
      return distCache.get(cacheKey);
    }
    int dist = StringUtils.getLevenshteinDistance(str1, str2);
    distCache.put(cacheKey, dist);
    return dist;
  }

  public static void trimLargeItems(int[] stringLengths, List<? extends Object> originals) {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    for (int i = 0; i < stringLengths.length; i++) {
      stats.addValue(stringLengths[i]);
    }
    double stdDev = stats.getStandardDeviation();
    double mean = stats.getMean();
    List<Object> toRemove = new ArrayList<Object>();
    for (int i = 0; i < stringLengths.length; i++) {
      double diff = stringLengths[i] - mean;
      if (diff / stdDev > 4d) {
        toRemove.add(originals.get(i));
      }
    }
    for (Object obj : toRemove) {
      originals.remove(obj);
    }
  }

  public static boolean isSame(List<String> lhs, List<String> rhs) {
    if (lhs == null && rhs == null) {
      return true;
    }
    if (lhs == null) {
      return false;
    }
    if (rhs == null) {
      return false;
    }
    if (lhs.size() != rhs.size()) {
      return false;
    }
    for (int i = 0; i < lhs.size(); i++) {
      if (!lhs.get(i).equals(rhs.get(i))) {
        return false;
      }
    }
    return true;
  }

  public static List<String> join(List<String> list1, List<String> list2) {
    List<String> list = new ArrayList<String>();
    for (String cur : list1) {
      if (!list.contains(cur)) {
        list.add(cur);
      }
    }
    for (String cur : list2) {
      if (!list.contains(cur)) {
        list.add(cur);
      }
    }
    return list;
  }
}
