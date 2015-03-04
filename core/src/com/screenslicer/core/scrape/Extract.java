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
package com.screenslicer.core.scrape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.core.scrape.neural.NeuralNetManager;
import com.screenslicer.core.scrape.type.ComparableNode;
import com.screenslicer.core.util.NodeUtil;

public class Extract {
  private static final int SCORE_PENALTY = 10000;
  private static final int TWICE = 2;
  private static HashMap<Element, ComparableNode[]> nodesCache = new HashMap<Element, ComparableNode[]>();

  private static class TrainingData {
    private final ComparableNode target;
    private int finalMisses = 0;
    private boolean winner = false;
    private int winnerDistance = 0;
    private ComparableNode best = null;

    public TrainingData(ComparableNode target) {
      this.target = target;
    }
  }

  private static ComparableNode best(ComparableNode[] nodes, Integer[][] comparisonCache, Collection<Node> ignore, TrainingData trainingData) {
    int ignoreSize = ignore == null ? 0 : ignore.size();
    if (nodes.length - ignoreSize == 1) {
      if (ignore == null || ignore.isEmpty()) {
        return nodes[0];
      }
      for (int i = 0; i < nodes.length; i++) {
        if (!ignore.contains(nodes[i])) {
          return nodes[i];
        }
      }
    }
    if (comparisonCache == null) {
      comparisonCache = new Integer[nodes.length][nodes.length];
    }
    int adjustedLen = nodes.length - ignore.size();
    for (int failMax = 0; failMax < adjustedLen; failMax++) {
      Map<ComparableNode, Integer> winners = new HashMap<ComparableNode, Integer>();
      for (int i = 0; i < nodes.length; i++) {
        if (ignore != null && ignore.contains(nodes[i].node())) {
          continue;
        }
        boolean found = true;
        int fail = 0;
        for (int j = 0; j < nodes.length; j++) {
          if (ignore != null && ignore.contains(nodes[j].node())) {
            continue;
          }
          if (nodes[j] != null) {
            if (comparisonCache[i][j] == null) {
              int result = nodes[i].compare(nodes[j]);
              if (result != -1) {
                ++fail;
              }
              comparisonCache[i][j] = new Integer(result);
              comparisonCache[j][i] = new Integer(result * (-1));
            } else if (comparisonCache[i][j].intValue() != -1) {
              ++fail;
            }
            if (fail > failMax) {
              found = false;
              break;
            }
          }
        }
        if (found) {
          if (failMax == 0) {
            if (trainingData != null) {
              trainingData.winner = trainingData.target.equals(nodes[i]);
              trainingData.finalMisses = trainingData.winner ? 0 : 1;
              trainingData.winnerDistance = 0;
              trainingData.best = nodes[i];
            }
            return nodes[i];
          }
          winners.put(nodes[i], i);
        }
      }
      if (winners.size() == 1) {
        ComparableNode ret = winners.keySet().toArray(new ComparableNode[1])[0];
        if (trainingData != null) {
          trainingData.winner = trainingData.target.equals(ret);
          trainingData.finalMisses = trainingData.winner ? 0 : 1;
          trainingData.winnerDistance = failMax;
          trainingData.best = ret;
        }
        return ret;
      }
      if (!winners.isEmpty()) {
        int targetIndex = -1;
        ComparableNode[] winnersArray = winners.keySet().toArray(new ComparableNode[0]);
        if (trainingData != null) {
          trainingData.winnerDistance = failMax;
          if (winners.containsKey(trainingData.target)) {
            for (int i = 0; i < winnersArray.length; i++) {
              if (trainingData.target.equals(winnersArray[i])) {
                targetIndex = i;
                break;
              }
            }
          }
          if (targetIndex == -1) {
            trainingData.finalMisses = winners.size();
            trainingData.winner = false;
          }
        }
        for (int i = 0; i < winnersArray.length; i++) {
          boolean found = true;
          for (int j = 0; j < winnersArray.length; j++) {
            if (i != j) {
              int iCache = winners.get(winnersArray[i]);
              int jCache = winners.get(winnersArray[j]);
              if (comparisonCache[iCache][jCache] == null) {
                int result = winnersArray[i].compare(winnersArray[j]);
                comparisonCache[iCache][jCache] = new Integer(result);
                comparisonCache[jCache][iCache] = new Integer(result * (-1));
              }
              if (comparisonCache[iCache][jCache].intValue() != -1) {
                found = false;
                if (i != targetIndex) {
                  break;
                } else if (trainingData != null) {
                  ++trainingData.finalMisses;
                }
              }
            }
          }
          if (found) {
            if (trainingData != null) {
              trainingData.best = winnersArray[i];
            }
            if (targetIndex == i && trainingData != null) {
              trainingData.finalMisses = 0;
              trainingData.winner = true;
              return trainingData.target;
            } else if (targetIndex == -1 || targetIndex < i) {
              if (trainingData != null) {
                trainingData.winner = false;
              }
              return winnersArray[i];
            }
          } else if (targetIndex == i
              && trainingData != null
              && trainingData.best != null) {
            trainingData.winner = false;
            return trainingData.best;
          }
        }
        return null;
      }
    }
    return null;
  }

  public static ComparableNode[] trainInit(Element body, int page, int thread) {
    ComparableNode[] nodesArray = performInternal(body, page, null, null, null, thread);
    nodesCache.put(body, nodesArray);
    return nodesArray;
  }

  public static int train(Element body, int page, ComparableNode target, int targetIndex, int thread) {
    ComparableNode[] nodesArray = null;
    nodesArray = nodesCache.get(body);
    int score = 0;
    if (NeuralNetManager.instance(thread).isMulti()) {
      int votes = 0;
      final int majority = (NeuralNetManager.instance(thread).multiSize() / TWICE) + 1;
      boolean won = false;
      ComparableNode fallback = null;
      int[] distances = new int[NeuralNetManager.instance(thread).multiSize()];
      int curDistance = 0;
      Map<ComparableNode, Integer> votesMap = new HashMap<ComparableNode, Integer>();
      if (targetIndex < 0) {
        for (int i = 0; i < nodesArray.length; i++) {
          if (nodesArray[i].equals(target)) {
            targetIndex = i;
            break;
          }
        }
      }
      while (NeuralNetManager.instance(thread).hasNext()) {
        Integer[][] comparisonCache = new Integer[nodesArray.length][nodesArray.length];
        int distance = 0;
        for (int i = 0; i < nodesArray.length; i++) {
          if (!target.equals(nodesArray[i])) {
            int result = target.compare(nodesArray[i]);
            if (result != -1) {
              ++distance;
            }
            comparisonCache[targetIndex][i] = new Integer(result);
            comparisonCache[i][targetIndex] = new Integer(result * (-1));
          }
        }
        TrainingData trainingData = new TrainingData(target);
        ComparableNode tmp = best(nodesArray, comparisonCache, null, trainingData);
        if (tmp != null) {
          fallback = tmp;
        }
        if (trainingData.best != null) {
          if (!votesMap.containsKey(trainingData.best)) {
            votesMap.put(trainingData.best, new Integer(1));
          } else {
            votesMap.put(trainingData.best,
                new Integer(votesMap.get(trainingData.best).intValue() + 1));
          }
        }
        distance = (distance - trainingData.winnerDistance) + trainingData.finalMisses;
        NeuralNetManager.instance(thread).next();
        if (trainingData.winner) {
          ++votes;
        }
        if (votes == majority) {
          won = true;
          break;
        }
        distances[curDistance++] = distance;
      }
      NeuralNetManager.instance(thread).resetNext();
      if (!won) {
        int maxVotes = 0;
        ComparableNode maxComparableNode = null;
        for (Map.Entry<ComparableNode, Integer> entry : votesMap.entrySet()) {
          if (entry.getValue().intValue() == maxVotes) {
            maxComparableNode = null;
          } else if (entry.getValue().intValue() > maxVotes) {
            maxVotes = entry.getValue().intValue();
            maxComparableNode = entry.getKey();
          }
        }
        if (maxComparableNode == null) {
          maxComparableNode = fallback;
        }
        if (!target.equals(maxComparableNode)) {
          int totalDistance = 0;
          Arrays.sort(distances);
          for (int i = 0; i < majority; i++) {
            totalDistance += distances[i];
          }
          score += totalDistance + SCORE_PENALTY;
        }
      }
    } else {
      int distance = 0;
      Integer[][] comparisonCache = new Integer[nodesArray.length][nodesArray.length];
      if (targetIndex < 0) {
        for (int i = 0; i < nodesArray.length; i++) {
          if (nodesArray[i].equals(target)) {
            targetIndex = i;
            break;
          }
        }
      }
      for (int i = 0; i < nodesArray.length; i++) {
        if (!target.equals(nodesArray[i])) {
          int result = target.compare(nodesArray[i]);
          if (result != -1) {
            ++distance;
          }
          comparisonCache[targetIndex][i] = new Integer(result);
          comparisonCache[i][targetIndex] = new Integer(result * (-1));
        }
      }
      TrainingData trainingData = new TrainingData(target);
      best(nodesArray, comparisonCache, null, trainingData);
      score += (distance - trainingData.winnerDistance) + trainingData.finalMisses;
      score += trainingData.winner ? 0 : SCORE_PENALTY;
    }
    return score;
  }

  private static ComparableNode[] performInternal(final Element body, final int page,
      final HtmlNode matchResult, final HtmlNode matchParent, final Collection<Node> ignore, int thread) {
    final Map<Node, ComparableNode> nodes = new HashMap<Node, ComparableNode>();
    if (body != null) {
      body.traverse(new NodeVisitor() {
        @Override
        public void head(Node node, int depth) {
          int nonEmptyChildren = 0;
          for (Node child : node.childNodes()) {
            if (!NodeUtil.isEmpty(child)) {
              nonEmptyChildren++;
            }
          }
          if (!NodeUtil.isEmpty(node)
              && NodeUtil.isContent(node, matchResult, matchParent) && nonEmptyChildren > 0) {
            nodes.put(node, new ComparableNode(node, matchResult, matchParent, thread));
          }
        }

        @Override
        public void tail(Node node, int depth) {}
      });
    }
    return nodes.values().toArray(new ComparableNode[0]);
  }

  public static class Cache {
    public ComparableNode[] nodesCache = null;
    public Integer[][][] comparisonCache = null;
  }

  public static List<Node> perform(Element body, int page, Collection<Node> ignore,
      HtmlNode matchResult, HtmlNode matchParent, Cache cache, int thread) {
    Map<ComparableNode, Integer> votes = new LinkedHashMap<ComparableNode, Integer>();
    if (cache == null) {
      cache = new Cache();
    }
    if (cache.nodesCache == null) {
      cache.nodesCache = performInternal(body, page, matchResult, matchParent, ignore, thread);
      cache.comparisonCache = new Integer[NeuralNetManager.instance(thread).multiSize()]
          [cache.nodesCache.length][cache.nodesCache.length];
    }
    final int majority = (NeuralNetManager.instance(thread).multiSize() / TWICE) + 1;
    Node best = null;
    int cur = 0;
    NeuralNetManager.instance(thread).resetNext();
    while (NeuralNetManager.instance(thread).hasNext()) {
      ComparableNode winner = best(cache.nodesCache, cache.comparisonCache[cur++],
          new HashSet<Node>(ignore), null);
      NeuralNetManager.instance(thread).next();
      if (winner != null) {
        if (!votes.containsKey(winner)) {
          votes.put(winner, new Integer(1));
        } else {
          votes.put(winner, new Integer(votes.get(winner).intValue() + 1));
        }
        if (votes.get(winner).intValue() == majority) {
          best = winner.node();
          break;
        }
      }
    }
    if (best == null) {
      int bestVotes = 0;
      List<Node> bestNodes = new ArrayList<Node>();
      for (Map.Entry<ComparableNode, Integer> entry : votes.entrySet()) {
        int val = entry.getValue().intValue();
        if (val >= bestVotes) {
          if (val > bestVotes) {
            bestVotes = val;
            bestNodes.clear();
          }
          bestNodes.add(entry.getKey().node());
        }
      }
      return bestNodes;
    }
    return Arrays.asList(new Node[] { best });
  }
}
