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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;
import org.openqa.selenium.WebElement;

import com.machinepublishers.browser.Browser;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.HtmlCoder;
import com.screenslicer.common.Log;
import com.screenslicer.core.scrape.Scrape.ActionFailed;
import com.screenslicer.core.util.BrowserUtil;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.StringUtil;

public class Proceed {
  private static String[] controls = new String[] { "a", "input", "button", "div", "li", "span", "footer" };
  private static Pattern controlLabels =
      Pattern
          .compile("\\b(?:next|older|下一页|التالية|suivant|weiter|अगला|avanti|次|следующая|siguiente)\\b", Pattern.UNICODE_CHARACTER_CLASS);
  private static Pattern controlLabelsAlt =
      Pattern
          .compile(
              "\\b\\w*\\s*(?:more\\sstories|more\\snews|more\\sresults|more\\ssearch\\sresults|show\\smore$|earlier|previous\\sposts?|previous|older\\sposts?|(?:show\\s)?next\\s(?:\\d+\\s)?result\\(?s?\\)?)\\s*\\w*\\b",
              Pattern.UNICODE_CHARACTER_CLASS);
  private static Pattern entities = Pattern.compile("&\\w+?;|\\u00BB|\\u203A", Pattern.UNICODE_CHARACTER_CLASS);
  private static final int MAX_PRIMARY_LEN = 30;

  public static Context perform(Element body, int pageNum) {
    Context context = perform(body, pageNum, null);
    if (context != null && context.node != null) {
      return context;
    }
    return new Context();
  }

  public static class End extends Exception {
    private static final long serialVersionUID = 1L;
  }

  public static boolean isRemovable(Node node, Node reference) {
    String nodeText = CommonUtil.strip(HtmlCoder.decode(text(node)), false).replaceAll("\\s", "");
    String referenceText = "";
    int dist = -1;
    if (reference != null) {
      referenceText = CommonUtil.strip(HtmlCoder.decode(text(reference)), false).replaceAll("\\s", "");
      dist = StringUtil.dist(nodeText, referenceText);
    }
    return nodeText.length() < MAX_PRIMARY_LEN && dist < referenceText.length() / 2;
  }

  public static String perform(Browser browser, HtmlNode[] proceedClicks, int pageNum, String priorTextLabel) throws End, ActionFailed {
    try {
      Element body = BrowserUtil.openElement(browser, true, null, null, null, null);
      String origSrc = browser.getPageSource();
      String origTitle = browser.getTitle();
      String origUrl = browser.getCurrentUrl();
      if (!CommonUtil.isEmpty(proceedClicks)) {
        BrowserUtil.doClicks(browser, proceedClicks, body, false);
        return null;
      } else {
        Context context = perform(body, pageNum, priorTextLabel);
        if (context != null && context.node != null) {
          WebElement element = BrowserUtil.toElement(browser, context.node);
          if (element != null) {
            boolean success = BrowserUtil.click(browser, element, false);
            if (success) {
              browser.getStatusCode();
              String newSource = browser.getPageSource();
              String newTitle = browser.getTitle();
              String newUrl = browser.getCurrentUrl();
              if (origSrc.hashCode() != newSource.hashCode()
                  || !origTitle.equals(newTitle)
                  || !origUrl.equals(newUrl)) {
                return context.textLabel;
              }
            }
          }
        }
      }
    } catch (Browser.Retry r) {
      throw r;
    } catch (Browser.Fatal f) {
      throw f;
    } catch (Throwable t) {
      Log.exception(t);
    }
    throw new End();
  }

  private static Context perform(Element body, int pageNum, String priorTextLabel) {
    Map<Node, String> nodeCache = new HashMap<Node, String>();
    Map<String, Integer> intCache = new HashMap<String, Integer>();
    Map<String, String> textControlHelperCache = new HashMap<String, String>();
    Context context = numberControl(body, pageNum, nodeCache, intCache);
    if (context == null) {
      context = textControl(body, false, controlLabels, textControlHelperCache, priorTextLabel, context);
      if (context == null) {
        context = textControl(body, true, controlLabels, textControlHelperCache, priorTextLabel, context);
        if (context == null) {
          context = textControl(body, false, controlLabelsAlt, textControlHelperCache, priorTextLabel, context);
          if (context == null) {
            context = textControl(body, true, controlLabelsAlt, textControlHelperCache, priorTextLabel, context);
          }
        }
      }
    }
    return context;
  }

  public static class Context {
    public String textLabel;
    public Node node;
    public Node proceedParent;
  }

  private static Context textControl(Element body, boolean title, Pattern labelPatterns,
      Map<String, String> cache, String priorTextLabel, Context context) {
    for (int i = 0; i < controls.length; i++) {
      Context target = textControlHelper(body, labelPatterns, controls[i], title, cache, priorTextLabel);
      if (target != null) {
        return target;
      }
    }
    return null;
  }

  private static Context textControlHelper(Element body, final Pattern label,
      final String controlName, final boolean title, final Map<String, String> cache,
      final String priorTextLabel) {
    final List<Context> textControls = new ArrayList<Context>();
    body.traverse(new NodeVisitor() {
      @Override
      public void tail(Node node, int depth) {}

      @Override
      public void head(Node node, int depth) {
        if (node.nodeName().equals(controlName) && !NodeUtil.isEmpty(node, false)) {
          String nodeStr = title ? title(node) : text(node);
          String text;
          if (cache.containsKey(nodeStr)) {
            text = cache.get(nodeStr);
          } else {
            text = CommonUtil.strip(entities.matcher(nodeStr).replaceAll(""), true)
                .replaceAll("\\p{Punct}", "").toLowerCase().trim();
            cache.put(nodeStr, text);
          }
          if (label.matcher(text).find()
              && label.matcher(text).replaceAll("").trim().indexOf(" ") == -1
              && (priorTextLabel == null || text.equalsIgnoreCase(priorTextLabel))) {
            Context context = new Context();
            context.node = node;
            context.textLabel = text;
            context.proceedParent = node;
            textControls.add(context);
          }
        }
      }
    });
    for (int i = 0; i < controls.length; i++) {
      for (Context context : textControls) {
        if (context.node.nodeName().equals(controls[i])) {
          return context;
        }
      }
    }
    return null;
  }

  private static Context numberControl(Element body, int pageNum, Map<Node, String> nodeCache, Map<String, Integer> intCache) {
    final Map<Node, Integer> numberLists = new HashMap<Node, Integer>();
    Node numberList = numberList(body, nodeCache, intCache, numberLists, true);
    while (numberList != null) {
      try {
        Node target;
        target = nodeWithText(numberList.childNodes(), Integer.toString(pageNum), nodeCache);
        for (int i = 0; i < controls.length; i++) {
          Node child = getNode(target, controls[i]);
          if (child != null) {
            Context context = new Context();
            context.node = child;
            context.proceedParent = numberList;
            return context;
          }
        }
        numberLists.remove(numberList);
        numberList = numberList(body, nodeCache, intCache, numberLists, false);
      } catch (Browser.Retry r) {
        throw r;
      } catch (Browser.Fatal f) {
        throw f;
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
    return null;
  }

  private static void numberListHelper(Node node, Map<Node, Integer> numberLists, Map<Node, String> nodeCache, Map<String, Integer> intCache) {
    int count = 0;
    int previousNum = -1;
    boolean first = true;
    for (Node child : node.childNodes()) {
      if (!NodeUtil.isEmpty(child, false)) {
        String nodeStr = null;
        if (nodeCache.containsKey(child)) {
          nodeStr = nodeCache.get(child);
        } else {
          nodeStr = CommonUtil.strip(text(child), true).replaceAll("\\p{Punct}", "");
          nodeCache.put(child, nodeStr);
        }
        Integer intVal = null;
        if (intCache.containsKey(nodeStr)) {
          intVal = intCache.get(nodeStr);
        } else {
          intVal = toInt(nodeStr);
          intCache.put(nodeStr, intVal);
        }
        if (intVal != null && first) {
          previousNum = intVal;
          ++count;
          first = false;
        } else if (intVal != null && intVal.intValue() == (previousNum + 1)) {
          ++previousNum;
          ++count;
        }
      }
    }
    if (count > 1) {
      numberLists.put(node, count);
    }
  }

  private static Node numberList(Element body,
      final Map<Node, String> nodeCache, final Map<String, Integer> intCache,
      final Map<Node, Integer> numberLists, boolean init) {
    if (init) {
      body.traverse(new NodeVisitor() {
        @Override
        public void tail(Node node, int depth) {}

        @Override
        public void head(Node node, int depth) {
          if (!NodeUtil.isEmpty(node, false)) {
            numberListHelper(node, numberLists, nodeCache, intCache);
          }
        }
      });
    }
    int maxCount = -1;
    Node maxNode = null;
    for (Map.Entry<Node, Integer> entry : numberLists.entrySet()) {
      if (entry.getValue().intValue() > maxCount) {
        maxNode = entry.getKey();
        maxCount = entry.getValue().intValue();
      }
    }
    return maxNode;
  }

  private static Node getNode(Node node, final String nodeName) {
    final List<Node> candidates = new ArrayList<Node>();
    if (node != null) {
      node.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int depth) {}

        @Override
        public void head(Node n, int depth) {
          if (!NodeUtil.isEmpty(n, false) && n.nodeName().equals(nodeName)) {
            candidates.add(n);
          }
        }
      });
      if (candidates.size() == 1) {
        return candidates.get(0);
      }
    }
    return null;
  }

  private static Node nodeWithText(List<Node> nodes, String str, Map<Node, String> nodeCache) {
    final List<Node> nodesWithText = new ArrayList<Node>();
    for (Node node : nodes) {
      if (!NodeUtil.isEmpty(node, false)) {
        String nodeStr = null;
        if (nodeCache.containsKey(node)) {
          nodeStr = nodeCache.get(node);
        } else {
          nodeStr = CommonUtil.strip(text(node), true).replaceAll("\\p{Punct}", "");
          nodeCache.put(node, nodeStr);
        }
        if (nodeStr.equals(str)) {
          nodesWithText.add(node);
        }
      }
    }
    if (nodesWithText.size() == 1) {
      return nodesWithText.get(0);
    }
    return null;
  }

  private static Integer toInt(String str) {
    try {
      int i = Integer.parseInt(CommonUtil.strip(str, true));
      return new Integer(i);
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  private static String text(Node node) {
    final StringBuilder stringBuilder = new StringBuilder();
    node.traverse(new NodeVisitor() {
      @Override
      public void tail(Node node, int depth) {}

      @Override
      public void head(Node node, int depth) {
        if (node.nodeName().equals("#text")
            && !NodeUtil.isHidden(node.parent())) {
          stringBuilder.append(node.toString());
        }
      }
    });
    return stringBuilder.toString();
  }

  private static String title(Node node) {
    final StringBuilder stringBuilder = new StringBuilder();
    node.traverse(new NodeVisitor() {
      @Override
      public void tail(Node node, int depth) {}

      @Override
      public void head(Node node, int depth) {
        if (!NodeUtil.isEmpty(node, false)) {
          String title = node.attr("title");
          String alt = node.attr("alt");
          if (!CommonUtil.isEmpty(title)) {
            stringBuilder.append(title);
          } else if (!CommonUtil.isEmpty(alt)) {
            stringBuilder.append(alt);
          }
        }
      }
    });
    return stringBuilder.toString();
  }
}
