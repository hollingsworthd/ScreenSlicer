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
package com.screenslicer.core.scrape.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.api.request.Query;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.nlp.NlpUtil;
import com.screenslicer.core.scrape.Dissect;
import com.screenslicer.core.scrape.Proceed;
import com.screenslicer.core.scrape.Proceed.Context;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.StringUtil;
import com.screenslicer.webapp.WebApp;

public class ScrapeResults {
  public static ScrapeResults resultsNull = new ScrapeResults();
  private static final int MIN_SUMMARY_LEN = 15;
  private static final int MIN_VALID_SUMMARY = 40;
  private static final int MIN_VALID_TITLE = 12;

  public static enum Leniency {
    None, Url, Title
  };

  private final Node nodeExtract;
  private final List<ScrapeResult> results;
  private final String query;
  private final boolean isNull;
  private Integer scoreCached = null;
  private Integer scoreNaiveCached = null;
  private int nextDist = -1;
  private double nextDistRatio = 1d;
  private final int position;

  private ScrapeResults() {
    this.isNull = true;
    this.nodeExtract = null;
    this.results = null;
    this.query = null;
    this.position = -1;
  }

  public ScrapeResults(Element body, int page, Node nodeExtract, int position, Leniency leniency,
      boolean trim, Query query, Map<String, Object> cache) {
    this.position = position;
    List<Node> nextNodes;
    if (cache.containsKey("nextNodes")) {
      nextNodes = (List<Node>) cache.get("nextNodes");
    } else {
      nextNodes = new ArrayList<Node>();
      cache.put("nextNodes", nextNodes);
      Context next1 = Proceed.perform(body, page + 1);
      if (next1.node != null) {
        nextNodes.add(Proceed.isRemovable(next1.proceedParent, null) ? next1.proceedParent : next1.node);
        Context next2 = Proceed.perform(body, page + 1);
        if (next2.node != null) {
          nextNodes.add(Proceed.isRemovable(next2.proceedParent, next1.proceedParent) ? next2.proceedParent : next2.node);
        }
      }
    }
    Map<Node, List<Node>> extractNodeChildrenCache;
    if (cache.containsKey("extractNodeChildren")) {
      extractNodeChildrenCache = (Map<Node, List<Node>>) cache.get("extractNodeChildren");
    } else {
      extractNodeChildrenCache = new HashMap<Node, List<Node>>();
      cache.put("extractNodeChildren", extractNodeChildrenCache);
    }
    List<Node> nodes;
    if (extractNodeChildrenCache.containsKey(nodeExtract)) {
      nodes = extractNodeChildrenCache.get(nodeExtract);
    } else {
      nodes = new ArrayList<Node>();
      extractNodeChildrenCache.put(nodeExtract, nodes);
      for (Node child : nodeExtract.childNodes()) {
        if (!NodeUtil.isEmpty(child)) {
          nodes.add(child);
        }
      }
    }
    this.nodeExtract = nodeExtract;
    if (leniency == Leniency.Url) {
      this.results = Dissect.perform(body, nodeExtract, query.requireResultAnchor, nodes, true,
          false, trim, query.matchResult, query.matchParent, cache);
    } else if (leniency == Leniency.Title) {
      this.results = Dissect.perform(body, nodeExtract, query.requireResultAnchor, nodes, false,
          true, trim, query.matchResult, query.matchParent, cache);
    } else {
      this.results = Dissect.perform(body, nodeExtract, query.requireResultAnchor, nodes, false,
          false, trim, query.matchResult, query.matchParent, cache);
    }
    update(nextNodes);
    this.query = query.isKeywordQuery() ? ((KeywordQuery) query).keywords : null;
    this.isNull = false;

    Map<Node, Double> nextDistCache = cache.containsKey("nextDistCache")
        ? (Map<Node, Double>) cache.get("nextDistCache") : new HashMap<Node, Double>();
    cache.put("nextDistCache", nextDistCache);
    if (nextDistCache.containsKey(nodeExtract)) {
      nextDistRatio = nextDistCache.get(nodeExtract);
    } else {
      final List<Node> allNodesHead = new ArrayList<Node>();
      final List<Node> allNodesTail = new ArrayList<Node>();
      body.traverse(new NodeVisitor() {
        @Override
        public void tail(Node n, int d) {
          allNodesTail.add(n);
        }

        @Override
        public void head(Node n, int d) {
          allNodesHead.add(n);
        }
      });
      List<Integer> nextLocs = new ArrayList<Integer>();
      for (Node next : nextNodes) {
        nextLocs.add(allNodesHead.indexOf(next));
        nextLocs.add(allNodesTail.indexOf(next));
      }
      final List<Node> resultNodes = new ArrayList<Node>();
      for (ScrapeResult result : results) {
        for (Node node : result.getNodes()) {
          node.traverse(new NodeVisitor() {
            @Override
            public void tail(Node n, int d) {}

            @Override
            public void head(Node n, int d) {
              resultNodes.add(n);
            }
          });
        }
      }
      for (Node node : resultNodes) {
        for (int i : nextLocs) {
          int curDist = Math.abs(allNodesHead.indexOf(node) - i);
          nextDist = curDist > -1 && curDist < nextDist ? curDist : nextDist;
          curDist = Math.abs(allNodesTail.indexOf(node) - i);
          nextDist = curDist > -1 && curDist < nextDist ? curDist : nextDist;
        }
      }
      if (nextDist != -1) {
        nextDistRatio = 1d - ((double) nextDist) / (((double) allNodesHead.size()) / 2d);
        nextDistRatio = nextDistRatio > 1d ? 1d : (nextDistRatio < 0d ? 0d : nextDistRatio);
      }
      nextDistCache.put(nodeExtract, nextDistRatio);
    }
  }

  private void update(List<Node> nextNodes) {
    List<Node> latest = new ArrayList<Node>();
    List<ScrapeResult> toRemove = new ArrayList<ScrapeResult>();
    for (ScrapeResult result : this.results) {
      if (NodeUtil.overlaps(result.getNodes(), nextNodes)) {
        toRemove.add(result);
      } else {
        latest.addAll(result.getNodes());
      }
    }
    for (ScrapeResult result : toRemove) {
      this.results.remove(result);
    }
  }

  public Node nodeExtract() {
    return nodeExtract;
  }

  private static boolean hasQuery(ScrapeResult result, String query) {
    StringBuilder builder = new StringBuilder();
    String title = result.title();
    String summary = result.summary();
    String url = result.url();
    if (!CommonUtil.isEmpty(title)) {
      builder.append(" ");
      builder.append(title);
      builder.append(" ");
    }
    if (!CommonUtil.isEmpty(summary)) {
      builder.append(" ");
      builder.append(summary);
      builder.append(" ");
    }
    if (!CommonUtil.isEmpty(url)) {
      String[] parts = url.split("\\p{Punct}");
      for (int i = 0; i < parts.length; i++) {
        builder.append(" ");
        builder.append(parts[i]);
        builder.append(" ");
      }
    }
    String text = builder.toString();
    return NlpUtil.hasStem(query, text);
  }

  public int fieldScore(boolean naive, boolean priorityScoring) {
    if (isNull) {
      return -1;
    }
    if (scoreCached == null || scoreNaiveCached == null) {
      int queryMatch = 0;
      Collection<String> titles = new HashSet<String>();
      Collection<String> summaries = new HashSet<String>();
      Collection<String> dates = new ArrayList<String>();
      Collection<String> allTitlesDict = new HashSet<String>();
      Collection<String> allSummariesDict = new HashSet<String>();
      final int MAX_COUNT = 30;
      int count = 0;
      int summaryLen = 0;
      for (ScrapeResult result : results) {
        ++count;
        if (count > MAX_COUNT) {
          break;
        }
        String title = result.title();
        String summary = result.summary();
        String date = result.date();
        if (!CommonUtil.isEmpty(title)) {
          titles.add(title);
          allTitlesDict.add(title.toLowerCase().trim());
        }
        if (!CommonUtil.isEmpty(summary)) {
          summaryLen += summary.length();
          summaries.add(summary);
          allSummariesDict.add(summary.toLowerCase().trim());
        }
        if (!CommonUtil.isEmpty(date)) {
          dates.add(date);
        }
        queryMatch += hasQuery(result, query) ? 1 : 0;
      }
      long dist = 0;
      long distMax = 0;
      String[] allTitles = allTitlesDict.toArray(new String[0]);
      String[] allSummaries = allSummariesDict.toArray(new String[0]);
      Collection<String> titlesUniqueDict = new HashSet<String>();
      Collection<String> summariesUniqueDict = new HashSet<String>();
      for (int i = 0; i < allTitles.length; i++) {
        titlesUniqueDict.add(CommonUtil.toString(NlpUtil.stems(allTitles[i], false, true), " "));
      }
      for (int i = 0; i < allSummaries.length; i++) {
        summariesUniqueDict.add(CommonUtil.toString(NlpUtil.stems(allSummaries[i], false, true), " "));
      }
      String[] titlesUnique = titlesUniqueDict.toArray(new String[0]);
      for (int i = 0; i < titlesUnique.length; i++) {
        for (int j = 0; j < titlesUnique.length; j++) {
          if (j == i) {
            continue;
          }
          int curMax = Math.min(titlesUnique[i].length(), titlesUnique[j].length());
          distMax += curMax;
          dist += Math.min(curMax, StringUtil.dist(titlesUnique[i], titlesUnique[j]));
        }
      }
      String[] summariesUnique = summariesUniqueDict.toArray(new String[0]);
      for (int i = 0; i < summariesUnique.length; i++) {
        for (int j = 0; j < summariesUnique.length; j++) {
          if (j == i) {
            continue;
          }
          int curMax = Math.min(summariesUnique[i].length(), summariesUnique[j].length());
          distMax += curMax;
          dist += Math.min(curMax, StringUtil.dist(summariesUnique[i], summariesUnique[j]));
        }
      }
      double distRatio = 1d - ((1d - Math.min(1d, (double) dist / (double) distMax)) * 1d);
      int scoreInt = 0;
      int titleCount = 0;
      int summaryCount = 0;
      for (String str : titles) {
        scoreInt += str.length() > MIN_VALID_TITLE ? 4 : 0;
        if (!CommonUtil.isEmpty(str)) {
          ++titleCount;
        }
      }
      for (String str : summaries) {
        scoreInt += str.length() > MIN_VALID_SUMMARY ? 4 : 0;
        if (!CommonUtil.isEmpty(str)) {
          ++summaryCount;
        }
      }
      scoreInt += (int) Math.rint(((double) (titleCount + summaryCount)) / 2d);
      scoreInt += dates.size() * 2;
      double size = Math.min(MAX_COUNT, results.size());
      double queryMatchRatio = 1d - ((1d - (((double) queryMatch) / size))
          / (((int) Math.rint(((double) summaryLen) / size)) < MIN_SUMMARY_LEN ? 3d : 1));
      double scoreNaive = scoreInt;
      scoreNaive = ((scoreNaive * distRatio) / 3d) + ((scoreNaive * queryMatchRatio) / 3d) + ((scoreNaive * nextDistRatio) / 3d);
      double score = position == 0 && results.size() > 3 && priorityScoring ? 1.8d * scoreNaive : scoreNaive;
      scoreNaiveCached = new Integer((int) Math.rint(Math.rint(10d * scoreNaive / (Math.cbrt(size) * Math.cbrt(size)))));
      scoreNaiveCached = scoreNaiveCached < 0 ? new Integer(0) : scoreNaiveCached;
      scoreCached = new Integer((int) Math.rint(Math.rint(Math.sqrt(((score * score) / size)) * Math.sqrt(score))));
      scoreCached = scoreCached < 0 ? new Integer(0) : scoreCached;
    }
    Log.debug("score-> " + (naive ? scoreNaiveCached : scoreCached) + " (size: " + results.size() + ")", WebApp.DEBUG);
    return naive ? scoreNaiveCached : scoreCached;
  }

  public List<ScrapeResult> results() {
    if (isNull) {
      return new ArrayList<ScrapeResult>();
    }
    return results;
  }
}
