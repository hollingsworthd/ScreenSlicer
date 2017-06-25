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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.type.ScrapeResult;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.UrlUtil;

public class Dissect {
  private static final int MIN_EXPECTED_FIELD = 10;
  private static final int MIN_EXPECTED_AVG_SUMMARY = 60;
  private static final int MIN_EXPECTED_SUMMARY = 20;
  private static final int MIN_EXPECTED_AVG_TITLE = 60;
  private static final int MIN_EXPECTED_TITLE = 20;
  private static final int TWICE = 2;
  private static final int CRITICAL_MASS = 7;
  private static final int BANNED_SYMBOLS_FILTER_PREREQ_RESULT_NUM = 2;
  private static final Pattern bannedSymbols = Pattern.compile("(?:»|\\u00BB|«|\\u00AB|›|\\u203A|‹|\\u2039)", Pattern.UNICODE_CHARACTER_CLASS);
  private static final double SIGNIFICANT_RESULTS_RATIO = .4d;
  private static final double HALF = .495d;
  private static final Pattern uncountedText = Pattern.compile("[\\d.\\u2026]|(?:\\b(?:first|prev|next|last|pages?|»|\\u00BB|«|\\u00AB|›|\\u203A|‹|\\u2039)\\b)", Pattern.CASE_INSENSITIVE
      | Pattern.UNICODE_CHARACTER_CLASS);
  public static final Pattern cssUrl = Pattern.compile("\\b[Uu]rl\\b|Url", Pattern.UNICODE_CHARACTER_CLASS);

  private static void dedupStrings(String[] strings, boolean fromLeft) {
    for (int i = 0; i < strings.length; i++) {
      if (strings[i] == null || strings[i].trim().isEmpty()) {
        return;
      }
    }
    int len = 0;
    for (int i = 0; i < strings.length; i++) {
      strings[i] = " " + (fromLeft ? strings[i] : StringUtils.reverse(strings[i])) + " ";
    }
    if (strings != null && strings.length > 0 && strings[0] != null) {
      for (int i = 0; i < strings[0].length(); i++) {
        char c = strings[0].charAt(i);
        boolean match = true;
        for (int j = 1; j < strings.length; j++) {
          if (strings[j] == null
              || i >= strings[j].length()
              || strings[j].charAt(i) != c) {
            match = false;
            break;
          }
        }
        if (match) {
          ++len;
        } else {
          break;
        }
      }
    }
    for (int i = 0; len > 0 && i < strings.length; i++) {
      if (!Character.isWhitespace(strings[i].charAt(len - 1))) {
        --len;
        i = 0;
      }
    }
    for (int i = 0; len > 0 && i < strings.length; i++) {
      strings[i] = strings[i].substring(len);
    }
    for (int i = 0; i < strings.length; i++) {
      strings[i] = (fromLeft ? strings[i] : StringUtils.reverse(strings[i])).trim();
    }
  }

  public static String nodeHash(Node node, List<Node> nodes, boolean lenientUrl, boolean lenientTitle) {
    StringBuilder position = new StringBuilder();
    Node cur = node;
    while (cur != null) {
      position.append("<<0>>");
      position.append(cur.hashCode());
      position.append("<<1>>");
      position.append(cur.nodeName());
      position.append("<<2>>");
      position.append(cur.childNodes().size());
      position.append("<<3>>");
      position.append(cur.siblingIndex());
      position.append("<<4>>");
      cur = cur.parent();
    }
    for (Node child : node.childNodes()) {
      position.append("<<c0>>");
      position.append(child.hashCode());
      position.append("<<c1>>");
      position.append(child.nodeName());
      position.append("<<c2>>");
      position.append(child.childNodes().size());
      position.append("<<c3>>");
      position.append(child.siblingIndex());
      position.append("<<c4>>");
    }
    if (nodes != null) {
      for (Node n : nodes) {
        position.append("<<s0>>");
        position.append(n.hashCode());
        position.append("<<s1>>");
        position.append(n.nodeName());
        position.append("<<s2>>");
        position.append(n.childNodes().size());
        position.append("<<s3>>");
        position.append(n.siblingIndex());
        position.append("<<s4>>");
      }
    }
    position.append("<<>>");
    position.append(NodeUtil.outerHtml(node).hashCode());
    return "dissectedResults-<<" + lenientUrl + ">>-<<" + lenientTitle + ">>-" + position.toString();
  }

  public static List<ScrapeResult> perform(Element body, Node parent, boolean requireResultAnchor, List<Node> nodes, boolean lenientUrl,
      boolean lenientTitle, boolean trim, HtmlNode matchResult, HtmlNode matchParent, Map<String, Object> cache) {
    String baseParentHash = nodeHash(parent, nodes, lenientUrl, lenientTitle);
    String parentHashTrim = "nodeList-<<trim=true>>" + baseParentHash;
    String parentHashNoTrim = "nodeList-<<trim=false>>" + baseParentHash;
    if (trim && cache.containsKey(parentHashTrim)) {
      return (List<ScrapeResult>) cache.get(parentHashTrim);
    }
    if (!trim && cache.containsKey(parentHashNoTrim)) {
      return (List<ScrapeResult>) cache.get(parentHashNoTrim);
    }
    List<ScrapeResult> noTrimDissected = new ArrayList<ScrapeResult>();
    List<ScrapeResult> dissected = new ArrayList<ScrapeResult>();
    double avgTitle = 0;
    double avgSummary = 0;
    if (trim && cache.containsKey(parentHashNoTrim)) {
      dissected = (List<ScrapeResult>) cache.get(parentHashNoTrim);
      avgTitle = (Double) cache.get("dissectAvgTitle>>" + baseParentHash);
      avgSummary = (Double) cache.get("dissectAvgSummary>>" + baseParentHash);
    } else {
      dissected = Expand.perform(body, parent, requireResultAnchor, nodes, lenientUrl,
          lenientTitle, matchResult, matchParent, cache);
      if (dissected.isEmpty()) {
        cache.put(parentHashTrim, dissected);
        cache.put(parentHashNoTrim, dissected);
        return dissected;
      }
      boolean useDDMMYYYY = true;
      for (ScrapeResult cur : dissected) {
        if (!cur.isDDMMYYYY()) {
          useDDMMYYYY = false;
          break;
        }
      }
      if (useDDMMYYYY) {
        for (ScrapeResult cur : dissected) {
          cur.useDDMMYYYY();
        }
      }
      for (ScrapeResult cur : dissected) {
        if (cur.title() != null) {
          avgTitle += cur.title().trim().length();
        }
        if (cur.summary() != null) {
          avgSummary += cur.summary().trim().length();
        }
      }
      avgTitle /= dissected.size();
      avgSummary /= dissected.size();
      if ((avgTitle < 1 && avgSummary > 1)
          || (avgTitle > avgSummary && avgSummary > MIN_EXPECTED_FIELD)) {
        for (ScrapeResult cur : dissected) {
          cur.swapTitleAndSummary();
        }
        double tmp = avgTitle;
        avgTitle = avgSummary;
        avgSummary = tmp;
      }
      int totalAlt = 0;
      for (ScrapeResult cur : dissected) {
        totalAlt += cur.isAltUrlAndTitle() ? 1 : 0;
      }
      boolean useAlt = totalAlt > dissected.size() / TWICE;
      for (ScrapeResult cur : dissected) {
        cur.useAltUrlAndTitle(useAlt);
      }
      List<Double> avgTitleFallbacks = new ArrayList<Double>();
      Map<String, Integer> dupTitles = new HashMap<String, Integer>();
      double bestTitleFallbackAvg = 0d;
      int bestTitleFallbackIndex = -1;
      for (ScrapeResult cur : dissected) {
        String title = cur.title();
        if (!dupTitles.containsKey(title)) {
          dupTitles.put(title, 0);
        }
        dupTitles.put(title, dupTitles.get(title).intValue() + 1);
        for (int i = 0; i < cur.altFallbackTitleCount(); i++) {
          if (i >= avgTitleFallbacks.size()) {
            avgTitleFallbacks.add(0d);
          }
          if (cur.title() != null) {
            double val = avgTitleFallbacks.get(i);
            val += cur.altFallbackTitle(i).trim().length();
            avgTitleFallbacks.remove(i);
            avgTitleFallbacks.add(i, val);
          }
        }
      }
      for (int i = 0; i < avgTitleFallbacks.size(); i++) {
        double val = avgTitleFallbacks.get(i);
        val /= dissected.size();
        avgTitleFallbacks.remove(i);
        avgTitleFallbacks.add(i, val);
      }
      for (int i = 0; i < avgTitleFallbacks.size(); i++) {
        if (avgTitleFallbacks.get(i) > bestTitleFallbackAvg) {
          bestTitleFallbackAvg = avgTitleFallbacks.get(i);
          bestTitleFallbackIndex = i;
        }
      }
      boolean doFallback = (int) Math.rint(bestTitleFallbackAvg) > (int) Math.rint(avgTitle);
      int third = (int) Math.rint(((double) dissected.size()) / 3d);
      Map<String, Integer> dupFallbackTitles = new HashMap<String, Integer>();
      for (ScrapeResult cur : dissected) {
        if (dupTitles.get(cur.title()) > third) {
          String fallbackTitle = cur.altFallbackTitle(bestTitleFallbackIndex);
          if (!dupFallbackTitles.containsKey(fallbackTitle)) {
            dupFallbackTitles.put(fallbackTitle, 0);
          }
          dupFallbackTitles.put(fallbackTitle, dupFallbackTitles.get(fallbackTitle).intValue() + 1);
        }
      }
      double newTitleLen = 0;
      for (ScrapeResult cur : dissected) {
        if (doFallback
            || (dupTitles.get(cur.title()) > third
            && dupFallbackTitles.get(cur.altFallbackTitle(bestTitleFallbackIndex)) < third)) {
          cur.useAltFallbackUrlAndTitle(bestTitleFallbackIndex);
        }
        String newTitle = cur.title();
        newTitleLen += newTitle == null ? 0 : newTitle.length();
      }
      avgTitle = newTitleLen / (double) dissected.size();
      cache.put("dissectAvgTitle>>" + baseParentHash, avgTitle);
      cache.put("dissectAvgSummary>>" + baseParentHash, avgSummary);
      for (ScrapeResult cur : dissected) {
        noTrimDissected.add(cur.copy());
      }
      cache.put(parentHashNoTrim, noTrimDissected);
    }
    if (!trim) {
      return noTrimDissected;
    }
    if (avgSummary > MIN_EXPECTED_AVG_SUMMARY || avgTitle > MIN_EXPECTED_AVG_TITLE) {
      List<ScrapeResult> toRemove = new ArrayList<ScrapeResult>();
      for (int i = 0; i < dissected.size(); i++) {
        String title = dissected.get(i).title();
        title = title == null ? "" : title;
        String summary = dissected.get(i).summary();
        String strippedSummary = summary == null ?
            "" : uncountedText.matcher(summary).replaceAll("").trim();
        if (title.length() < MIN_EXPECTED_TITLE
            && strippedSummary.length() < MIN_EXPECTED_SUMMARY) {
          toRemove.add(dissected.get(i));
        }
      }
      for (ScrapeResult cur : toRemove) {
        dissected.remove(cur);
      }
    }
    Map<String, ScrapeResult> unique = new LinkedHashMap<String, ScrapeResult>();
    for (ScrapeResult cur : dissected) {
      unique.put(cur.url(), cur);
    }
    dissected = new ArrayList<ScrapeResult>(unique.values());
    if (avgSummary > MIN_EXPECTED_FIELD || avgTitle > MIN_EXPECTED_FIELD) {
      Map<String, Integer> count = new HashMap<String, Integer>();
      for (ScrapeResult cur : dissected) {
        String key = cur.title() + "<<>>" + cur.summary() + "<<>>" + cur.date();
        if (count.containsKey(key)) {
          count.put(key, count.get(key).intValue() + 1);
        } else {
          count.put(key, 1);
        }
      }
      double size = (double) dissected.size();
      List<ScrapeResult> toRemove = new ArrayList<ScrapeResult>();
      for (Map.Entry<String, Integer> entry : count.entrySet()) {
        if (((double) entry.getValue()) / size > SIGNIFICANT_RESULTS_RATIO) {
          for (ScrapeResult cur : dissected) {
            String key = cur.title() + "<<>>" + cur.summary() + "<<>>" + cur.date();
            if (key.equals(entry.getKey())) {
              toRemove.add(cur);
            }
          }
        }
      }
      for (ScrapeResult cur : toRemove) {
        dissected.remove(cur);
      }
    }
    if (dissected.size() > BANNED_SYMBOLS_FILTER_PREREQ_RESULT_NUM) {
      boolean firstBanned = false;
      boolean lastBanned = false;
      if (isBanned(dissected.get(0))) {
        firstBanned = true;
      }
      if (isBanned(dissected.get(dissected.size() - 1))) {
        lastBanned = true;
      }
      boolean otherBanned = false;
      for (int i = 1; i < dissected.size() - 1; i++) {
        if (isBanned(dissected.get(i))) {
          otherBanned = true;
          break;
        }
      }
      if (!otherBanned) {
        if (firstBanned) {
          dissected.remove(0);
        }
        if (lastBanned) {
          dissected.remove(dissected.size() - 1);
        }
      }

      int numRelative = 0;
      for (ScrapeResult cur : dissected) {
        if (isRelativeUrl(cur.url())) {
          ++numRelative;
        }
      }
      double size = (double) dissected.size();
      List<ScrapeResult> toRemove = new ArrayList<ScrapeResult>();
      if ((((double) numRelative) / size) < HALF) {
        for (ScrapeResult cur : dissected) {
          if (isRelativeUrl(cur.url())) {
            toRemove.add(cur);
          }
        }
        for (ScrapeResult cur : toRemove) {
          dissected.remove(cur);
        }
      }
      toRemove = new ArrayList<ScrapeResult>();
      for (int i = 0; i < dissected.size(); i++) {
        boolean found = true;
        for (int j = 0; j < dissected.size(); j++) {
          if (i != j
              && !dissected.get(j).url().contains(dissected.get(i).url())) {
            found = false;
            break;
          }
        }
        if (found) {
          toRemove.add(dissected.get(i));
        }
      }
      for (ScrapeResult cur : toRemove) {
        dissected.remove(cur);
      }
      toRemove = new ArrayList<ScrapeResult>();
      int nullSummary = 0;
      for (ScrapeResult cur : dissected) {
        if (CommonUtil.isEmpty(cur.summary())) {
          ++nullSummary;
        }
      }
      if (((double) nullSummary) / ((double) dissected.size()) < SIGNIFICANT_RESULTS_RATIO) {
        for (int i = 0; i < dissected.size(); i++) {
          if (CommonUtil.isEmpty(dissected.get(i).summary())) {
            toRemove.add(dissected.get(i));
          } else {
            break;
          }
        }
        for (int i = dissected.size() - 1; i >= 0; i--) {
          if (CommonUtil.isEmpty(dissected.get(i).summary())) {
            toRemove.add(dissected.get(i));
          } else {
            break;
          }
        }
        for (ScrapeResult cur : toRemove) {
          dissected.remove(cur);
        }
      }
    }
    if (dissected.size() > CRITICAL_MASS) {
      int oneWordTitles = 0;
      int hashUrls = 0;
      List<ScrapeResult> toRemoveTitles = new ArrayList<ScrapeResult>();
      List<Integer> removedTitles = new ArrayList<Integer>();
      List<Integer> keptTitles = new ArrayList<Integer>();
      List<ScrapeResult> toRemoveUrls = new ArrayList<ScrapeResult>();
      List<Integer> removedUrls = new ArrayList<Integer>();
      List<Integer> keptUrls = new ArrayList<Integer>();
      int index = 0;
      int firstRemovedTitle = -1;
      int firstRemovedUrl = -1;
      for (ScrapeResult cur : dissected) {
        if (cur.title() == null || cur.title().indexOf(' ') == -1) {
          ++oneWordTitles;
          toRemoveTitles.add(cur);
          removedTitles.add(index);
          firstRemovedTitle = index;
        } else {
          keptTitles.add(index);
        }
        if (cur.url().indexOf('#') != -1) {
          ++hashUrls;
          toRemoveUrls.add(cur);
          removedUrls.add(index);
          firstRemovedUrl = index;
        } else {
          keptUrls.add(index);
        }
        ++index;
      }
      int numOddTitles = 0;
      int numEvenTitles = 0;
      int numOddUrls = 0;
      int numEvenUrls = 0;
      boolean considerTitles = false;
      boolean considerUrls = false;
      if (oneWordTitles == dissected.size() / 2) {
        for (int i : removedTitles) {
          considerTitles = true;
          if (i % 2 == 0) {
            ++numEvenTitles;
          } else {
            ++numOddTitles;
          }
        }
        if (considerTitles && (numOddTitles == 0 || numEvenTitles == 0)) {
          numEvenTitles = 0;
          numOddTitles = 0;
          for (int i : keptTitles) {
            if (i % 2 == 0) {
              ++numEvenTitles;
            } else {
              ++numOddTitles;
            }
          }
        } else {
          considerTitles = false;
        }
      }
      if (hashUrls == dissected.size() / 2) {
        for (int i : removedUrls) {
          considerUrls = true;
          if (i % 2 == 0) {
            ++numEvenUrls;
          } else {
            ++numOddUrls;
          }
        }
        if (considerUrls && (numOddUrls == 0 || numEvenUrls == 0)) {
          numEvenUrls = 0;
          numOddUrls = 0;
          for (int i : keptUrls) {
            if (i % 2 == 0) {
              ++numEvenUrls;
            } else {
              ++numOddUrls;
            }
          }
        } else {
          considerUrls = false;
        }
      }
      int firstRemoved = -1;
      List<ScrapeResult> toRemoveResults = null;
      if (considerTitles && (numOddTitles == 0 || numEvenTitles == 0)) {
        toRemoveResults = toRemoveTitles;
        firstRemoved = firstRemovedTitle;
      } else if (considerUrls && (numOddUrls == 0 || numEvenUrls == 0)) {
        toRemoveResults = toRemoveUrls;
        firstRemoved = firstRemovedUrl;
      }
      if (toRemoveResults != null) {
        for (ScrapeResult cur : toRemoveResults) {
          int i = dissected.indexOf(cur);
          i += firstRemoved == 0 ? 1 : -1;
          if (i < dissected.size() && i > -1) {
            if (!CommonUtil.isEmpty(cur.summary())) {
              dissected.get(i).summaryMerge(cur.summaryNodes());
            }
            if (!CommonUtil.isEmpty(cur.date())) {
              dissected.get(i).setDate(cur.date());
            }
            List<Node> curNodes = cur.getNodes();
            for (Node curNode : curNodes) {
              dissected.get(i).addLast(curNode);
            }
          }
          dissected.remove(cur);
        }
      }
    }
    String[] summaries = new String[dissected.size()];
    int summaryIndex = 0;
    for (ScrapeResult result : dissected) {
      summaries[summaryIndex++] = result.summary();
    }
    dedupStrings(summaries, true);
    dedupStrings(summaries, false);
    summaryIndex = 0;
    for (ScrapeResult result : dissected) {
      result.setSummary(summaries[summaryIndex++]);
    }
    NodeUtil.trimLargeResults(dissected);
    cache.put(parentHashTrim, dissected);
    return dissected;
  }

  private static boolean isRelativeUrl(String url) {
    return url == null || (!url.startsWith("//") && !url.contains("://"));
  }

  private static boolean isBanned(ScrapeResult result) {
    return (result.title() != null && bannedSymbols.matcher(result.title()).find())
        || (result.summary() != null && bannedSymbols.matcher(result.summary()).find());
  }

  public static class Visitor implements NodeVisitor {
    public ScrapeResult result = new ScrapeResult();
    private int insideAnchor = 0;
    private Node insideLenientUrl = null;
    private Pattern tag = Pattern.compile("<[^>]+>.*$");
    private final boolean lenientUrl;
    private final boolean lenientTitle;
    private List<String> texts = new ArrayList<String>();
    private Map<String, Node> urls = new LinkedHashMap<String, Node>();
    private Collection<Node> seenHead = new HashSet<Node>();
    private Collection<Node> seenTail = new HashSet<Node>();
    private Collection<Node> seen = new HashSet<Node>();
    private boolean visited = false;

    public Visitor(Node node, boolean lenientUrl, boolean lenientTitle) {
      if (node != null) {
        result.addLast(node);
        seen.add(node);
      }
      this.lenientUrl = lenientUrl;
      this.lenientTitle = lenientTitle;
    }

    public Visitor(List<Node> nodes, boolean lenientUrl, boolean lenientTitle) {
      if (nodes != null) {
        for (Node node : nodes) {
          result.addLast(node);
          seen.add(node);
        }
      }
      this.lenientUrl = lenientUrl;
      this.lenientTitle = lenientTitle;
    }

    public void addFirst(Node node) {
      if (seen.contains(node)) {
        return;
      }
      seen.add(node);
      result.addFirst(node);
      visited = false;
    }

    public void addLast(Node node) {
      if (seen.contains(node)) {
        return;
      }
      seen.add(node);
      result.addLast(node);
      visited = false;
    }

    public void visit() {
      if (!visited) {
        for (Node node : result.getNodes()) {
          node.traverse(this);
          close();
        }
      }
      visited = true;
    }

    private void close() {
      if (!urls.isEmpty()) {
        String url = urls.keySet().iterator().next();
        Node urlNode = urls.get(url);
        if (!CommonUtil.isEmpty(url)) {
          for (String text : texts) {
            result.addUrl(urlNode, url, text, true, false, false, false);
          }
        }
        urls.clear();
      }
      insideAnchor = 0;
      texts.clear();
    }

    @Override
    public void tail(Node node, int depth) {
      if (seenTail.contains(node)) {
        return;
      }
      seenTail.add(node);
      if (!NodeUtil.isEmpty(node)) {
        if (node.nodeName().equals("a") && !lenientUrl) {
          --insideAnchor;
        }
      }
      if (node.equals(insideLenientUrl)) {
        insideLenientUrl = null;
      }
    }

    @Override
    public void head(final Node node, int depth) {
      if (seenHead.contains(node)) {
        return;
      }
      if (NodeUtil.isEmpty(node)) {
        seenHead.add(node);
      } else {
        if (node.nodeName().equals("a") && !lenientUrl) {
          boolean textSibling = false;
          for (Node sibling : node.siblingNodes()) {
            final boolean[] found = new boolean[1];
            sibling.traverse(new NodeVisitor() {
              private int valid = 0;

              @Override
              public void tail(Node n, int d) {
                if (!NodeUtil.isDecoration(n.nodeName()) && !n.nodeName().equals("#text")) {
                  --valid;
                }
              }

              @Override
              public void head(Node n, int d) {
                if (NodeUtil.isDecoration(n.nodeName()) || n.nodeName().equals("#text")) {
                  if (valid == 0 && n.nodeName().equals("#text") && !NodeUtil.isEmpty(n)) {
                    found[0] = true;
                  }
                } else {
                  ++valid;
                }
              }
            });
            if (found[0]) {
              textSibling = true;
              break;
            }
          }
          boolean anchorSibling = false;
          for (Node sibling : node.siblingNodes()) {
            final boolean[] found = new boolean[1];
            sibling.traverse(new NodeVisitor() {
              private int valid = 0;

              @Override
              public void tail(Node n, int d) {
                if (!NodeUtil.isDecoration(n.nodeName())
                    && !n.nodeName().equals("#text") && !n.nodeName().equals("a")) {
                  --valid;
                }
              }

              @Override
              public void head(Node n, int d) {
                if (NodeUtil.isDecoration(n.nodeName())
                    || n.nodeName().equals("#text") || n.nodeName().equals("a")) {
                  if (valid == 0 && n.nodeName().equals("a")) {
                    found[0] = true;
                  }
                } else {
                  ++valid;
                }
              }
            });
            if (found[0]) {
              anchorSibling = true;
              break;
            }
          }
          ++insideAnchor;
          boolean loneBlock = true;
          boolean loneBlockAttempted = false;
          Node anchorParent = node.parent();
          if (anchorParent != null) {
            for (Node curChild : anchorParent.childNodes()) {
              loneBlockAttempted = true;
              if (!NodeUtil.isEmpty(curChild) && !curChild.equals(node)) {
                loneBlock = false;
                break;
              }
            }
          }
          loneBlock = !loneBlockAttempted ? false : loneBlock;
          final List<StringBuilder> titles = new ArrayList<StringBuilder>();
          final List<String> imageTitles = new ArrayList<String>();
          titles.add(new StringBuilder());
          node.traverse(new NodeVisitor() {
            @Override
            public void tail(Node child, int depth) {}

            @Override
            public void head(Node child, int depth) {
              if (seenHead.contains(child)) {
                return;
              }
              seenHead.add(child);
              if (!NodeUtil.isEmpty(child) && child.nodeName().equals("#text")
                  && !NodeUtil.isBlock(child.parent().nodeName())) {
                String childStr = child.toString();
                if (!childStr.trim().toLowerCase().startsWith("&lt;img ")
                    && !childStr.trim().toLowerCase().startsWith("<img ")) {
                  titles.get(0).append(childStr);
                }
              } else if (!NodeUtil.isEmpty(child) && child.nodeName().equals("#text")) {
                String childStr = child.toString();
                if (!childStr.trim().toLowerCase().startsWith("&lt;img ")
                    && !childStr.trim().toLowerCase().startsWith("<img ")) {
                  titles.add(new StringBuilder(childStr));
                }
              } else if (!child.attr("title").trim().isEmpty()) {
                String title = child.attr("title").trim();
                title = tag.matcher(title).replaceFirst("");
                if (child.nodeName().equals("a")) {
                  titles.add(new StringBuilder(title));
                } else {
                  imageTitles.add(title);
                }
              } else if (!child.attr("alt").trim().isEmpty()) {
                String title = child.attr("alt").trim();
                title = tag.matcher(title).replaceFirst("");
                if (child.nodeName().equals("a")) {
                  titles.add(new StringBuilder(title));
                } else {
                  imageTitles.add(title);
                }
              }
            }
          });
          boolean hasTitle = false;
          for (StringBuilder title : titles) {
            String titleStr = title.toString();
            String hrefStr = node.attr("href");
            if (!CommonUtil.isEmpty(titleStr) && !CommonUtil.isEmpty(hrefStr)
                && !hrefStr.startsWith("javascript:")) {
              hasTitle = true;
              result.addUrl(node, node.attr("href"), titleStr, textSibling, anchorSibling, loneBlock, !imageTitles.isEmpty());
            }
          }
          if (!hasTitle && lenientTitle) {
            for (String title : imageTitles) {
              String hrefStr = node.attr("href");
              if (!CommonUtil.isEmpty(title) && !CommonUtil.isEmpty(hrefStr)
                  && !hrefStr.startsWith("javascript:")) {
                hasTitle = true;
                result.addUrl(node, node.attr("href"), title, textSibling, anchorSibling, loneBlock, true);
              }
            }
          }
        } else if (!NodeUtil.isEmpty(node)
            && node.nodeName().equals("#text")
            && insideAnchor == 0
            && insideLenientUrl == null) {
          if (seenHead.contains(node)) {
            return;
          }
          seenHead.add(node);
          if (lenientUrl) {
            texts.add(node.toString());
          } else {
            result.addToSummary(node.toString(), false, node);
          }
        } else if (!NodeUtil.isEmpty(node)
            && !node.nodeName().equals("a")
            && insideAnchor == 0) {
          if (seenHead.contains(node)) {
            return;
          }
          seenHead.add(node);
          String urlFromAttr = UrlUtil.urlFromAttr(node);
          if (Dissect.cssUrl.matcher(node.attr("class")).find()
              || (lenientUrl && !CommonUtil.isEmpty(urlFromAttr))) {
            if (lenientUrl && !CommonUtil.isEmpty(urlFromAttr) && !NodeUtil.isFilteredLenient(node)) {
              urls.put(urlFromAttr, node);
            }
            insideLenientUrl = node;
          }
        }
      }
    }
  }
}
