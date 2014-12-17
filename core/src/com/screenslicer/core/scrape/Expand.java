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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.Dissect.Visitor;
import com.screenslicer.core.scrape.type.ScrapeResult;
import com.screenslicer.core.util.Util;

public class Expand {
  private static final int MAX_CHILDREN = 7;
  private static final int MAX_GENERATIONS = 7;
  private static final int MAX_OVERLAP = 2;
  private static final int MIN_GROUP = 4;
  private static final double SIBLING_RATIO = .745;

  public static List<ScrapeResult> perform(Element body, Node parent, boolean requireResultAnchor,
      List<Node> nodes, boolean lenientUrl, boolean lenientTitle, HtmlNode matchResult,
      HtmlNode matchParent, Map<String, Object> cache) {
    if (nodes == null) {
      return null;
    }
    String cacheKey = "expanded-" + Dissect.nodeHash(parent, nodes, lenientUrl, false);
    List<Visitor> visitors = new ArrayList<Visitor>();
    if (cache.containsKey(cacheKey)) {
      List<List<Node>> retNodes = (List<List<Node>>) cache.get(cacheKey);
      for (List<Node> cur : retNodes) {
        visitors.add(new Visitor(cur, lenientUrl, lenientTitle));
      }
    } else {
      List<Node> trimmed = new ArrayList<Node>();
      List<Node> drillDown = drillDown(requireResultAnchor, nodes, lenientUrl, lenientTitle);
      HashSet<Node> topLevel = new HashSet<Node>(nodes);
      List<Node> baseline = drillUp(drillDown, topLevel, matchResult, matchParent);
      List<Node> current = null;
      if (baseline.size() < MIN_GROUP) {
        current = drillDown;
      } else {
        current = drillUp(baseline, topLevel, matchResult, matchParent);
        double prevSize = baseline.size();
        double curSize = current.size();
        while (current.size() >= MIN_GROUP && curSize >= Math.sqrt(prevSize)
            && !same(baseline, current)) {
          baseline = current;
          current = drillUp(baseline, topLevel, matchResult, matchParent);
          prevSize = baseline.size();
          curSize = current.size();
        }
        if (current.size() < MIN_GROUP || curSize < Math.sqrt(prevSize)) {
          current = baseline;
        }
      }
      Util.trimLargeNodes(current);
      current = Backfill.perform(body, current);
      Util.trimLargeNodes(current);
      visitInit(requireResultAnchor, current,
          trimmed, visitors, lenientUrl, lenientTitle);
      List<Node> expanded = Expand.findAncestors(trimmed, 0, matchResult, matchParent);
      if (expanded != null) {
        visitInit(requireResultAnchor, expanded, trimmed, visitors, lenientUrl, lenientTitle);
      }
      List<Node> allSiblings = new ArrayList<Node>();
      for (int i = 1; i < Integer.MAX_VALUE && storeSibling(findSiblings(trimmed, i, allSiblings), visitors, true); i++);
      for (int i = -1; i > Integer.MIN_VALUE && storeSibling(findSiblings(trimmed, i, allSiblings), visitors, false); i--);
      List<List<Node>> retNodes = new ArrayList<List<Node>>();
      for (Visitor visitor : visitors) {
        retNodes.add(visitor.result.getNodes());
      }
      cache.put(cacheKey, retNodes);
    }
    for (Visitor visitor : visitors) {
      visitor.visit();
    }
    List<ScrapeResult> results = new ArrayList<ScrapeResult>();
    for (Visitor visitor : visitors) {
      if (!visitor.result.isEmpty(requireResultAnchor)) {
        results.add(visitor.result);
      }
    }
    Util.trimLargeResults(results);
    return results;
  }

  private static boolean same(List<Node> lhs, List<Node> rhs) {
    if (lhs == null && rhs == null) {
      return true;
    }
    if (lhs == null || rhs == null) {
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

  private static List<Node> drillDown(boolean requireResultAnchor, List<Node> nodes,
      final boolean lenientUrl, boolean lenientTitle) {
    if (!requireResultAnchor) {
      return new ArrayList<Node>(nodes);
    }
    final Collection<Node> links = new LinkedHashSet<Node>();
    for (Node node : nodes) {
      node.traverse(new NodeVisitor() {

        @Override
        public void tail(Node n, int d) {}

        @Override
        public void head(Node n, int d) {
          boolean link = false;
          if (n.nodeName().equals("a")) {
            link = true;
          } else {
            String urlFromAttr = Util.urlFromAttr(n);
            if (Dissect.cssUrl.matcher(n.attr("class")).find()
                || (lenientUrl && !CommonUtil.isEmpty(urlFromAttr))) {
              if (lenientUrl && !CommonUtil.isEmpty(urlFromAttr) && !Util.isFilteredLenient(n)) {
                link = true;
              }
            }
          }
          if (link) {
            links.add(n);
          }
        }
      });
    }
    return new ArrayList<Node>(links);
  }

  private static List<Node> drillUp(Collection<Node> nodes, HashSet<Node> topLevel,
      final HtmlNode matchResult, final HtmlNode matchParent) {
    final Map<Node, List<Node>> parents = new LinkedHashMap<Node, List<Node>>();
    for (Node node : nodes) {
      Node parent = node;
      while (parent != null
          && !topLevel.contains(parent)
          && (!Util.isItem(parent, matchResult, matchParent) || parent.equals(node))) {
        parent = parent.parent();
      }
      if (parent != null && Util.isItem(parent, matchResult, matchParent)) {
        if (!parents.containsKey(parent)) {
          parents.put(parent, new ArrayList<Node>());
        }
        if (Util.isItem(node, matchResult, matchParent)) {
          parents.get(parent).add(node);
        }
      }
    }
    final Collection<Node> toRemoveParents = new HashSet<Node>();
    final Collection<Node> toRemoveChildren = new HashSet<Node>();
    final Collection<Node> toKeep = new HashSet<Node>();
    for (Node node : parents.keySet()) {
      final Node thisNode = node;
      node.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int d) {}

        @Override
        public void head(Node n, int d) {
          if (!n.equals(thisNode) && parents.containsKey(n)) {
            toRemoveParents.add(thisNode);
            toRemoveChildren.add(n);
          } else if (Util.isItem(n, matchResult, matchParent)) {
            toKeep.add(n);
          }
        }
      });
    }
    List<Node> ret = new ArrayList<Node>(parents.keySet());
    List<Node> finalRet = new ArrayList<Node>();
    for (int i = 0; i < ret.size(); i++) {
      List<Node> cur = parents.get(ret.get(i));
      if (toRemoveParents.contains(ret.get(i))) {
        Collection<Integer> sigs = new HashSet<Integer>();
        for (Node node : cur) {
          sigs.add(signature(node));
        }
        if (!cur.isEmpty()
            && ((cur.size() < MIN_GROUP && sigs.size() == 1)
            || (sigs.size() <= cur.size() / MIN_GROUP))) {
          finalRet.addAll(cur);
        } else {
          finalRet.add(ret.get(i));
        }
      } else if (!toRemoveChildren.contains(ret.get(i))) {
        if (cur.size() >= MIN_GROUP) {
          Collection<Integer> sigs = new HashSet<Integer>();
          for (Node node : cur) {
            sigs.add(signature(node));
          }
          if (sigs.size() < cur.size() - 1) {
            finalRet.addAll(cur);
          } else {
            finalRet.add(ret.get(i));
          }
        } else {
          finalRet.add(ret.get(i));
        }
      }
    }
    return finalRet;
  }

  private static int signature(Node node) {
    final Collection<Node> nodes = new HashSet<Node>();
    final Collection<String> names = new HashSet<String>();
    node.traverse(new NodeVisitor() {
      @Override
      public void tail(Node n, int d) {}

      @Override
      public void head(Node n, int d) {
        if (Util.isBlock(n.nodeName())) {
          nodes.add(n);
          names.add(n.nodeName());
        }
      }
    });
    String[] nameStrs = names.toArray(new String[0]);
    Arrays.sort(nameStrs);
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < nameStrs.length; i++) {
      builder.append(nameStrs[i]);
    }
    return builder.toString().hashCode();
  }

  private static void visitInit(boolean requireResultAnchor, List<Node> nodes, List<Node> trimmed,
      List<Visitor> visitors, final boolean lenientUrl, boolean lenientTitle) {
    trimmed.clear();
    visitors.clear();
    for (Node node : nodes) {
      final boolean[] empty = new boolean[] { true };
      if (!requireResultAnchor) {
        empty[0] = false;
      } else {
        node.traverse(new NodeVisitor() {
          @Override
          public void tail(Node n, int d) {}

          @Override
          public void head(Node n, int d) {
            if (empty[0]) {
              if (n.nodeName().equals("a")) {
                empty[0] = false;
              } else {
                String urlFromAttr = Util.urlFromAttr(n);
                if (Dissect.cssUrl.matcher(n.attr("class")).find()
                    || (lenientUrl && !CommonUtil.isEmpty(urlFromAttr))) {
                  if (lenientUrl && !CommonUtil.isEmpty(urlFromAttr) && !Util.isFilteredLenient(n)) {
                    empty[0] = false;
                  }
                }
              }
            }
          }
        });
      }
      if (!empty[0]) {
        Visitor visitor = new Visitor(node, lenientUrl, lenientTitle);
        trimmed.add(node);
        visitors.add(visitor);
      }
    }
  }

  private static boolean storeSibling(List<Node> siblings, List<Visitor> visitors, boolean forward) {
    if (siblings == null || siblings.isEmpty()) {
      return false;
    }
    for (int i = 0; i < siblings.size(); i++) {
      Visitor visitor = visitors.get(i);
      Node sibling = siblings.get(i);
      if (sibling != null) {
        if (forward) {
          visitor.addLast(sibling);
        } else {
          visitor.addFirst(sibling);
        }
      }
    }
    return true;
  }

  private static List<Node> findSiblings(List<Node> nodes, int distance, List<Node> allSiblings) {
    List<Node> siblings = new ArrayList<Node>();
    int found = 0;
    for (Node node : nodes) {
      Node parent = node.parent();
      Node sibling = null;
      if (parent != null) {
        int index = node.siblingIndex();
        if (index + distance < parent.childNodeSize() && index + distance > -1) {
          sibling = parent.childNode(index + distance);
        }
        if (sibling != null && (nodes.contains(sibling) || allSiblings.contains(sibling))) {
          sibling = null;
        }
      }
      if (sibling != null) {
        ++found;
        siblings.add(sibling);
      } else {
        siblings.add(null);
      }
    }
    if (((double) found) / ((double) nodes.size()) > SIBLING_RATIO) {
      allSiblings.addAll(siblings);
      return siblings;
    }
    return null;
  }

  private static List<Node> findAncestors(List<Node> nodes, int generation,
      HtmlNode matchResult, HtmlNode matchParent) {
    if (nodes == null) {
      return null;
    }
    if (nodes.isEmpty()) {
      if (generation == 0) {
        return null;
      }
      return nodes;
    }
    boolean end = false;
    int contained = 0;
    for (Node node : nodes) {
      if (node.parent() == null || !Util.isItem(node.parent(), matchResult, matchParent)) {
        end = true;
        break;
      }
      List<Node> siblings = new ArrayList<Node>(node.parent().childNodes());
      if (contains(siblings, node, nodes)) {
        ++contained;
        if (contained > nodes.size() / MAX_OVERLAP) {
          end = true;
        }
      }
    }
    if (!end) {
      List<Node> expanded = new ArrayList<Node>();
      for (Node node : nodes) {
        expanded.add(node.parent());
      }
      for (Node node : expanded) {
        int childNodes = 0;
        Node parent = node.parent();
        if (parent == null) {
          return expanded;
        }
        for (Node child : parent.childNodes()) {
          if (!Util.isEmpty(child)) {
            ++childNodes;
          }
        }
        if (childNodes > MAX_CHILDREN || generation > MAX_GENERATIONS) {
          return expanded;
        }
      }
      return findAncestors(expanded, generation + 1, matchResult, matchParent);
    } else {
      if (generation == 0) {
        return null;
      }
      return nodes;
    }
  }

  private static boolean contains(final List<Node> targets, Node self, final List<Node> containers) {
    final Collection<Node> containerDescendants = new HashSet<Node>();
    for (Node container : containers) {
      container.traverse(new NodeVisitor() {
        @Override
        public void tail(Node node, int level) {}

        @Override
        public void head(Node node, int level) {
          containerDescendants.add(node);
        }
      });
    }
    final Collection<Node> targetDescendants = new HashSet<Node>();
    for (Node target : targets) {
      target.traverse(new NodeVisitor() {
        @Override
        public void tail(Node node, int level) {}

        @Override
        public void head(Node node, int level) {
          targetDescendants.add(node);
        }
      });
    }
    targetDescendants.remove(self);
    for (Node target : targetDescendants) {
      if (containerDescendants.contains(target)) {
        return true;
      }
    }
    return false;
  }
}
