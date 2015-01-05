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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.Result;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.type.ScrapeResult;

public class NodeUtil {
  private static final String[] blocks = new String[] { "address", "article", "aside", "audio", "blockquote", "canvas", "dd", "div", "dl", "fieldset", "figcaption", "figure", "footer", "form", "h1",
      "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "noscript", "ol", "output", "p", "pre", "section", "table", "tr", "tfoot", "ul", "video" };
  private static final String[] proximityBlocks = new String[] { "address", "article", "aside", "audio", "blockquote", "canvas", "dd", "div", "dl", "fieldset", "figcaption", "figure", "footer",
      "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "noscript", "ol", "output", "p", "pre", "section", "table", "tr", "td", "tfoot", "ul", "video" };
  private static final String[] formatting = new String[] { "article", "h1", "h2", "h3", "h4", "h5", "h6", "header", "footer", "address", "p", "hr", "blockquote", "dt", "dd", "div", "a", "em",
      "strong", "small", "cite", "q", "dfn", "abbr", "time", "var", "i", "b", "u", "mark", "bdi", "bdo", "span", "br", "wbr", "img", "video", "audio", "source", "track", "svg", "ol", "ul", "li",
      "dl", "table", "caption", "colgroup", "col", "tbody", "thead", "tfoot", "tr", "td", "th", "section", "main" };
  private static final String[] decoration = new String[] { "abbr", "acronym", "address", "applet", "area", "aside", "b", "bdi", "bdo", "big", "blink", "br", "caption", "cite",
      "code", "data", "datalist", "em", "embed", "figcaption", "figure", "font", "i", "img", "kbd", "label", "link", "map", "mark", "marquee", "meta", "meter", "nobr",
      "noframes", "noscript", "output", "param", "plaintext", "pre", "q", "rp", "rt", "ruby", "s", "samp", "script", "small", "source", "spacer", "span", "strike", "strong", "style", "sub",
      "summary", "sup", "template", "#text", "time", "var", "video", "wbr" };
  private static final String NODE_MARKER = "fftheme_";
  private static final Pattern nodeMarker = Pattern.compile(NODE_MARKER + "\\d+");
  private static final String HIDDEN_MARKER = "xmoztheme";
  private static final String FILTERED_MARKER = "o2xtheme";
  private static final Pattern filteredMarker = Pattern.compile(FILTERED_MARKER);
  private static final String FILTERED_LENIENT_MARKER = "o2x2theme";
  private static final Pattern filteredLenientMarker = Pattern.compile(FILTERED_LENIENT_MARKER);
  private static final Pattern hiddenMarker = Pattern.compile(HIDDEN_MARKER);
  private static final int MAX_HTML_CACHE = 10000;
  private static final Map<Node, String> htmlCache = new HashMap<Node, String>(MAX_HTML_CACHE);
  private static final String[] items = new String[] { "address", "article", "dd", "dt", "div", "p", "section", "table", "tr", "li", "td", "fieldset", "form", "h1", "h2", "h3", "h4", "h5", "h6", };
  private static final String[] content = new String[] { "p", "ol", "ul", "dl", "div", "colgroup", "tbody", "td", "section", "main", "form" };

  public static boolean isBlock(String name) {
    for (int i = 0; i < blocks.length; i++) {
      if (name.equalsIgnoreCase(blocks[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isProximityBlock(String name) {
    for (int i = 0; i < proximityBlocks.length; i++) {
      if (name.equalsIgnoreCase(proximityBlocks[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isFormatting(String name) {
    for (int i = 0; i < formatting.length; i++) {
      if (name.equalsIgnoreCase(formatting[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isDecoration(String name) {
    for (int i = 0; i < decoration.length; i++) {
      if (name.equalsIgnoreCase(decoration[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isContent(Node node, HtmlNode matchResult, HtmlNode matchParent) {
    if (matchParent != null) {
      return matches(matchParent, node);
    }
    if (matchResult != null) {
      for (Node child : node.childNodes()) {
        if (matches(matchResult, child)) {
          return true;
        }
      }
      return false;
    }
    for (int i = 0; i < content.length; i++) {
      if (node.nodeName().equalsIgnoreCase(content[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isItem(Node node, HtmlNode matchResult, HtmlNode matchParent) {
    if (matchResult != null) {
      return matches(matchResult, node);
    }
    if (matchParent != null) {
      return node.parent() != null && matches(matchParent, node.parent());
    }
    for (int i = 0; i < items.length; i++) {
      if (node.nodeName().equalsIgnoreCase(items[i])) {
        return true;
      }
    }
    return false;
  }

  public static boolean isHidden(Node node) {
    return node.attr("class").indexOf(HIDDEN_MARKER) > -1;
  }

  private static boolean isFiltered(Node node) {
    return node.attr("class").indexOf(FILTERED_MARKER) > -1;
  }

  public static boolean isFilteredLenient(Node node) {
    return node.attr("class").indexOf(FILTERED_MARKER) > -1
        || node.attr("class").indexOf(FILTERED_LENIENT_MARKER) > -1;
  }

  public static boolean isEmpty(Node node) {
    return isEmpty(node, true);
  }

  public static boolean isEmpty(Node node, boolean doFilter) {
    return node == null
        || node.nodeName().equals("#comment")
        || node.nodeName().equals("#data")
        || node.nodeName().equals("style")
        || node.nodeName().equals("script")
        || isHidden(node)
        || (doFilter && isFiltered(node))
        || (node.nodeName().equals("#text")
        && CommonUtil.isEmpty(node.toString(), true));
  }

  public static boolean isResultFiltered(Result result, String[] whitelist, String[] patterns, HtmlNode[] urlNodes) {
    return UrlUtil.isUrlFiltered(null, result.url, CommonUtil.parseFragment(result.urlNode, false), whitelist, patterns, urlNodes, null);
  }

  public static boolean overlaps(List<Node> nodes, List<Node> targets) {
    final Collection<Node> all = new HashSet<Node>();
    for (Node target : targets) {
      target.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int d) {}

        @Override
        public void head(Node n, int d) {
          all.add(n);
        }
      });
    }
    final boolean overlaps[] = new boolean[1];
    for (Node node : nodes) {
      node.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int d) {}

        @Override
        public void head(Node n, int d) {
          if (!overlaps[0]) {
            if (all.contains(n)) {
              overlaps[0] = true;
            }
          }
        }
      });
      if (overlaps[0]) {
        return true;
      }
    }
    return false;
  }

  static boolean matches(HtmlNode reference, Node test) {
    if (test == null) {
      return false;
    }
    if (!CommonUtil.isEmpty(reference.id)) {
      return reference.id.equalsIgnoreCase(test.attr("id"));
    }
    if (!CommonUtil.isEmpty(reference.name)) {
      return reference.name.equalsIgnoreCase(test.attr("name"));
    }
    List<String[]> toMatch = new ArrayList<String[]>();
    toMatch.add(new String[] { reference.tagName, test.nodeName() });
    toMatch.add(new String[] { reference.type, test.attr("type") });
    toMatch.add(new String[] { reference.value, test.attr("value") });
    toMatch.add(new String[] { reference.title, test.attr("title") });
    toMatch.add(new String[] { reference.alt, test.attr("alt") });
    toMatch.add(new String[] { reference.href, test.attr("href") });
    if (test instanceof Element) {
      toMatch.add(new String[] { CommonUtil.strip(reference.innerText, false),
          CommonUtil.strip(((Element) test).text(), false) });
    }
    String refClassesString = CommonUtil.toString(reference.classes, " ");
    Collection<String> refClasses = new HashSet<String>(Arrays.asList(refClassesString.toLowerCase().split("\\s")));
    Collection<String> testClasses = new HashSet<String>(Arrays.asList(test.attr("class").toLowerCase().split("\\s")));
    for (String[] pair : toMatch) {
      if (reference.any) {
        if (!CommonUtil.isEmpty(pair[0]) && pair[0].equalsIgnoreCase(pair[1])) {
          return true;
        }
      } else {
        if (!CommonUtil.isEmpty(pair[0]) && !pair[0].equalsIgnoreCase(pair[1])) {
          return false;
        }
      }
    }
    if (!refClasses.isEmpty()) {
      for (String testClass : testClasses) {
        if (reference.any) {
          if (refClasses.contains(testClass)) {
            return true;
          }
        } else {
          if (!refClasses.contains(testClass)) {
            return false;
          }
        }
      }
    }
    return false;
  }

  public static int nearestBlock(Node node) {
    int nearest = 0;
    Node parent = node.parent();
    while (parent != null) {
      ++nearest;
      if (NodeUtil.isProximityBlock(parent.nodeName())) {
        return nearest;
      }
      parent = parent.parent();
    }
    return Integer.MAX_VALUE;
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

  public static void trimLargeResults(List<ScrapeResult> results) {
    int[] stringLengths = new int[results.size()];
    for (int i = 0; i < results.size(); i++) {
      stringLengths[i] = NodeUtil.outerHtml(results.get(i).getNodes()).length();
    }
    StringUtil.trimLargeItems(stringLengths, results);
  }

  public static void trimLargeNodes(List<Node> nodes) {
    int[] stringLengths = new int[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) {
      stringLengths[i] = NodeUtil.outerHtml(nodes.get(i)).length();
    }
    StringUtil.trimLargeItems(stringLengths, nodes);
  }

  public static Element markTestElement(Element element) {
    element.traverse(new NodeVisitor() {
      @Override
      public void tail(Node node, int level) {}

      @Override
      public void head(Node node, int level) {
        node.attr("class", nodeMarker.matcher(node.attr("class")).replaceAll(""));
      }
    });
    element.traverse(new NodeVisitor() {
      int count = 0;

      @Override
      public void tail(Node node, int level) {}

      @Override
      public void head(Node node, int level) {
        ++count;
        node.attr("class", node.attr("class") + " " + NODE_MARKER + count + " ");
      }
    });
    return element;
  }

  static void markFiltered(Node node, final boolean lenient) {
    if (lenient) {
      if (!isFilteredLenient(node)) {
        node.attr("class", node.attr("class") + " " + FILTERED_LENIENT_MARKER + " ");
      }
    } else {
      node.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int d) {}

        @Override
        public void head(Node n, int d) {
          if (!isFiltered(n)) {
            n.attr("class", n.attr("class") + " " + FILTERED_MARKER + " ");
          }
        }
      });
    }
  }

  static void markVisible(Node node) {
    if (node != null) {
      if (node.nodeName().equals("select")) {
        node.traverse(new NodeVisitor() {
          @Override
          public void tail(Node n, int d) {}

          @Override
          public void head(Node n, int d) {
            n.attr("class", hiddenMarker.matcher(n.attr("class")).replaceAll(""));
          }
        });
      }
      node.attr("class", hiddenMarker.matcher(node.attr("class")).replaceAll(""));
      markVisible(node.parent());
    }
  }

  public static void clearOuterHtmlCache() {
    htmlCache.clear();
  }

  public static String outerHtml(List<Node> nodes) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; nodes != null && i < nodes.size(); i++) {
      builder.append(nodes.get(i).outerHtml());
      if (i + 1 < nodes.size()) {
        builder.append("\n");
      }
    }
    return builder.toString().trim();
  }

  public static String outerHtml(Node node) {
    if (htmlCache.containsKey(node)) {
      return htmlCache.get(node);
    }
    String html = node.outerHtml();
    if (htmlCache.size() == MAX_HTML_CACHE) {
      htmlCache.clear();
    }
    htmlCache.put(node, html);
    return html;
  }

  static String classId(Node node) {
    if (node != null) {
      String className = node.attr("class");
      if (!CommonUtil.isEmpty(className)) {
        Matcher matcher = nodeMarker.matcher(className);
        if (matcher.find()) {
          return matcher.group(0);
        }
      }
    }
    return null;
  }

  public static void clean(List<Node> nodes) {
    for (Node node : nodes) {
      clean(node);
    }
  }

  public static Document clean(String string, String url) {
    Document doc = CommonUtil.parse(string, url, false);
    NodeUtil.clean(doc.childNodes());
    return doc;
  }

  private static void clean(Node node) {
    node.traverse(new NodeVisitor() {
      @Override
      public void tail(Node node, int depth) {}

      @Override
      public void head(Node node, int depth) {
        String classAttr = node.attr("class");
        classAttr = NodeUtil.cleanClass(classAttr);
        if (CommonUtil.isEmpty(classAttr)) {
          node.removeAttr("class");
        } else {
          node.attr("class", classAttr);
        }
      }
    });
  }

  public static String cleanClass(String classStr) {
    return nodeMarker.matcher(
        hiddenMarker.matcher(
            filteredMarker.matcher(
                filteredLenientMarker.matcher(
                    classStr).replaceAll("")).replaceAll("")).replaceAll("")).replaceAll("")
        .replaceAll("\\s+", " ").trim();
  }
}
