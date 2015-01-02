/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.util.NodeUtil;

import edu.emory.mathcs.backport.java.util.Arrays;

public class Backfill {
  private static final int MAX_CSS_CLASSES = 2;

  public static List<Node> perform(Element body, List<Node> results) {
    if (results != null
        && body != null && !results.isEmpty()) {
      String nodeName = null;
      boolean first = true;
      boolean found = true;
      Collection<String> classAttrs = new HashSet<String>();
      for (Node node : results) {
        String curNodeName = node.nodeName();
        String curClassName = NodeUtil.cleanClass(node.attr("class"));
        if (!classAttrs.contains(curClassName)) {
          classAttrs.add(curClassName);
        }
        if (first) {
          nodeName = curNodeName;
        } else if (CommonUtil.isEmpty(nodeName) || CommonUtil.isEmpty(curNodeName)
            || !curNodeName.equalsIgnoreCase(nodeName)) {
          found = false;
          break;
        }
        first = false;
      }
      if (classAttrs.size() > MAX_CSS_CLASSES || classAttrs.isEmpty()) {
        found = false;
      }
      if (found) {
        Collection<Node> resultSet = new LinkedHashSet<Node>();
        for (Node node : results) {
          resultSet.add(node);
        }
        Collection<String> classNames = new HashSet<String>();
        for (String className : classAttrs) {
          String[] cur = className.split("\\s");
          for (int i = 0; i < cur.length; i++) {
            classNames.add(cur[i]);
          }
        }
        Collection<Node> matching = new LinkedHashSet<Node>();
        for (String className : classNames) {
          if (!CommonUtil.isEmpty(className)) {
            Elements elements = body.getElementsByAttributeValueContaining("class", className);
            for (Element element : elements) {
              String foundClass = NodeUtil.cleanClass(element.attr("class"));
              if (element.nodeName().equalsIgnoreCase(nodeName)
                  && foundClass.contains(className)) {
                if ((matching.contains(element))
                    && !resultSet.contains(element)) {
                  resultSet.add(element);
                } else {
                  matching.add(element);
                }
              }
            }
          }
        }
        return removeCycles(reorder(body, resultSet), results);
      }
    }
    return removeCycles(reorder(body, results), results);
  }

  private static List<Node> removeCycles(List<Node> nodes, List<Node> originalNodes) {
    Collection<Node> set = new LinkedHashSet<Node>(nodes);
    Collection<Node> originals = new HashSet<Node>(originalNodes);
    for (Node node : nodes) {
      Node parent = node.parent();
      while (parent != null) {
        if (set.contains(parent)) {
          if (originals.contains(node)) {
            set.remove(parent);
            break;
          } else if (originals.contains(parent)) {
            set.remove(node);
            break;
          } else {
            set.remove(parent);
            break;
          }
        }
        parent = parent.parent();
      }
    }
    List<Node> list = new ArrayList<Node>();
    for (Node node : set) {
      list.add(node);
    }
    return list;
  }

  private static List<Node> reorder(Element body, Collection<Node> nodes) {
    final String bodyStr = NodeUtil.outerHtml(body).replaceAll("\\s", "");
    Map<Integer, Node> map = new HashMap<Integer, Node>();
    int[] indices = new int[nodes.size()];
    List<Node> extras = new ArrayList<Node>();
    int cur = 0;
    for (Node node : nodes) {
      final String nodeStr = NodeUtil.outerHtml(node).replaceAll("\\s", "");
      int i = bodyStr.indexOf(nodeStr);
      if (map.containsKey(i)) {
        extras.add(node);
      } else {
        map.put(i, node);
      }
      indices[cur++] = i;
    }
    Arrays.sort(indices);
    List<Node> ret = new ArrayList<Node>();
    for (int i = 0; i < indices.length; i++) {
      if (map.containsKey(indices[i])) {
        ret.add(map.get(indices[i]));
      }
    }
    ret.addAll(extras);
    return ret;
  }
}
