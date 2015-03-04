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
package com.screenslicer.core.scrape.type;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jsoup.nodes.Node;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.core.scrape.neural.NeuralNetManager;
import com.screenslicer.core.util.NodeUtil;
import com.screenslicer.core.util.StringUtil;

public class ComparableNode {
  private static final int RESULT_GROUP_SMALL = 5;
  private static final int RESULT_GROUP_LARGE = 10;
  private static final double FLOAT_SIGNIFICANT_DIGITS = 1000d;
  private static final double FLOAT_ROUNDING_ERR = .000001d;
  private static final double SIGNIFICANT_DIFF = .125d;
  private final Node node;
  private final int[] scores;
  private final int thread;

  public ComparableNode(final Node node, HtmlNode matchResult, HtmlNode matchParent, int thread) {
    this.thread = thread;
    this.node = node;
    List<Node> separated = node.childNodes();
    int children = 0;
    int childBlocks = 0;
    int childFormatting = 0;
    int childContent = 0;
    int childItems = 0;
    int childDecoration = 0;
    int anchorChildren = 0;
    int textChildren = 0;
    int anchorTextChildren = 0;
    int anchorChildItems = 0;
    int textChildItems = 0;
    int anchorTextChildItems = 0;
    int itemChars = 0;
    int itemAnchorChars = 0;
    List<String> firstChildTags = null;
    List<List<String>> orderedTags = new ArrayList<List<String>>();
    List<String> allChildTags = new ArrayList<String>();
    ArrayList<List<String>> childTags = new ArrayList<List<String>>();
    boolean childrenConsistent = true;
    String childName = null;
    boolean childrenSame = true;
    double avgChildLengthDouble = 0d;
    int nodeStrLen = NodeUtil.trimmedLen(node.toString());
    DescriptiveStatistics statAnchorChars = new DescriptiveStatistics();
    DescriptiveStatistics statAnchors = new DescriptiveStatistics();
    DescriptiveStatistics statChars = new DescriptiveStatistics();
    DescriptiveStatistics statDescendants = new DescriptiveStatistics();
    DescriptiveStatistics statFields = new DescriptiveStatistics();
    DescriptiveStatistics statLevels = new DescriptiveStatistics();
    DescriptiveStatistics statLongestField = new DescriptiveStatistics();
    DescriptiveStatistics statNonAnchorChars = new DescriptiveStatistics();
    DescriptiveStatistics statTextAnchors = new DescriptiveStatistics();
    DescriptiveStatistics statStrLen = new DescriptiveStatistics();
    DescriptiveStatistics statItemChars = new DescriptiveStatistics();
    DescriptiveStatistics statItemAnchorChars = new DescriptiveStatistics();
    for (Node child : separated) {
      if (!NodeUtil.isEmpty(child)) {
        children++;
        int childStrLen = NodeUtil.trimmedLen(child.toString());
        avgChildLengthDouble += childStrLen;
        NodeCounter counter = new NodeCounter(child, matchResult, matchParent);
        if (NodeUtil.isItem(child, matchResult, matchParent)) {
          ++childItems;
          anchorChildItems += counter.anchors() > 0 ? 1 : 0;
          textChildItems += counter.fields() > 0 ? 1 : 0;
          anchorTextChildItems += counter.anchors() > 0 && counter.fields() > 0 ? 1 : 0;
          itemChars += counter.chars();
          itemAnchorChars += counter.anchorChars();
          statItemChars.addValue(counter.chars());
          statItemAnchorChars.addValue(counter.anchorChars());
        }
        if (NodeUtil.isBlock(child.nodeName())) {
          ++childBlocks;
        }
        if (NodeUtil.isDecoration(child.nodeName())) {
          ++childDecoration;
        }
        if (NodeUtil.isFormatting(child.nodeName())) {
          ++childFormatting;
        }
        if (NodeUtil.isContent(child, matchResult, matchParent)) {
          ++childContent;
        }

        anchorChildren += counter.anchors() > 0 ? 1 : 0;
        textChildren += counter.fields() > 0 ? 1 : 0;
        anchorTextChildren += counter.anchors() > 0 && counter.fields() > 0 ? 1 : 0;

        statAnchorChars.addValue(counter.anchorChars());
        statAnchors.addValue(counter.anchors());
        statChars.addValue(counter.chars());
        statDescendants.addValue(counter.descendants());
        statFields.addValue(counter.fields());
        statLevels.addValue(counter.levels());
        statLongestField.addValue(counter.longestField());
        statNonAnchorChars.addValue(counter.nonAnchorChars());
        statTextAnchors.addValue(counter.textAnchors());
        statStrLen.addValue(childStrLen);

        List<String> curChildTags = counter.tags();
        allChildTags = StringUtil.join(allChildTags, curChildTags);
        childTags.add(curChildTags);
        if (firstChildTags == null) {
          firstChildTags = curChildTags;
        } else if (childrenConsistent && !StringUtil.isSame(firstChildTags, curChildTags)) {
          childrenConsistent = false;
        }

        if (childName == null) {
          childName = child.nodeName();
        } else if (childrenSame && !childName.equals(child.nodeName())) {
          childrenSame = false;
        }

        if (!StringUtil.contains(counter.orderedTags(), orderedTags)) {
          orderedTags.add(counter.orderedTags());
        }
      }
    }
    avgChildLengthDouble = children == 0 ? 0 : avgChildLengthDouble / (double) children;
    int avgChildLength = (int) avgChildLengthDouble;
    double avgChildDiff = 0;
    int maxChildDiff = 0;
    for (List<String> tagList : childTags) {
      avgChildDiff += allChildTags.size() - tagList.size();
      maxChildDiff = Math.max(maxChildDiff, allChildTags.size() - tagList.size());
    }
    avgChildDiff = childTags.size() == 0 ? 0 : avgChildDiff / (double) childTags.size();

    childrenConsistent = firstChildTags != null && !firstChildTags.isEmpty() && childrenConsistent;

    NodeCounter counter = new NodeCounter(separated, matchResult, matchParent);
    int siblings = 0;
    for (Node sibling : node.parent().childNodes()) {
      if (!NodeUtil.isEmpty(sibling)) {
        siblings++;
      }
    }
    this.scores = new int[] {
        counter.items(),
        counter.blocks(),
        counter.decoration(),
        counter.formatting(),
        counter.content(),
        div(counter.items(), children),
        div(counter.blocks(), children),
        div(counter.decoration(), children),
        div(counter.formatting(), children),
        div(counter.content(), children),

        childItems,
        childBlocks,
        childDecoration,
        childFormatting,
        childContent,
        avgChildLength,

        counter.fields(),
        textChildItems,
        counter.images(),
        counter.anchors(),
        counter.textAnchors(),
        div(counter.chars(), Math.max(1, counter.fields())),
        div(itemChars, Math.max(1, textChildItems)),

        counter.longestField(),
        nodeStrLen,
        div(nodeStrLen, children),
        counter.anchorLen(),
        counter.chars(),
        itemChars,
        div(counter.chars(), children),
        div(itemChars, childItems),
        counter.nonAnchorChars(),
        div(counter.nonAnchorChars(), children),
        div(counter.nonAnchorChars(), childItems),
        div(counter.nonAnchorChars(), childBlocks),
        div(counter.nonAnchorChars(), childContent),
        div(counter.nonAnchorChars(), counter.anchors()),
        div(counter.nonAnchorChars(), counter.textAnchors()),
        counter.anchorChars(),
        itemAnchorChars,
        div(itemAnchorChars, anchorChildItems),
        div(counter.anchorChars(), counter.anchors()),
        div(counter.anchorChars(), counter.textAnchors()),
        div(counter.anchorChars(), children),

        counter.descendants(), counter.levels(),
        div(counter.descendants(), children),
        div(children, counter.levels()),
        siblings,
        children,

        maxChildDiff,
        toInt(avgChildDiff),
        toInt(childrenSame),
        toInt(childrenConsistent),
        orderedTags.size(),

        mod0(children, RESULT_GROUP_LARGE),
        mod0(children, RESULT_GROUP_SMALL),
        distance(children, RESULT_GROUP_LARGE),
        distance(children, RESULT_GROUP_SMALL),
        mod0(childItems, RESULT_GROUP_LARGE),
        mod0(childItems, RESULT_GROUP_SMALL),
        distance(childItems, RESULT_GROUP_LARGE),
        distance(childItems, RESULT_GROUP_SMALL),
        mod0(childBlocks, RESULT_GROUP_LARGE),
        mod0(childBlocks, RESULT_GROUP_SMALL),
        distance(childBlocks, RESULT_GROUP_LARGE),
        distance(childBlocks, RESULT_GROUP_SMALL),
        mod0(childContent, RESULT_GROUP_LARGE),
        mod0(childContent, RESULT_GROUP_SMALL),
        distance(childContent, RESULT_GROUP_LARGE),
        distance(childContent, RESULT_GROUP_SMALL),
        mod0(counter.anchors(), RESULT_GROUP_LARGE),
        mod0(counter.anchors(), RESULT_GROUP_SMALL),
        distance(counter.anchors(), RESULT_GROUP_LARGE),
        distance(counter.anchors(), RESULT_GROUP_SMALL),
        mod0(anchorChildItems, RESULT_GROUP_LARGE),
        mod0(anchorChildItems, RESULT_GROUP_SMALL),
        distance(anchorChildItems, RESULT_GROUP_LARGE),
        distance(anchorChildItems, RESULT_GROUP_SMALL),
        mod0(textChildItems, RESULT_GROUP_LARGE),
        mod0(textChildItems, RESULT_GROUP_SMALL),
        distance(textChildItems, RESULT_GROUP_LARGE),
        distance(textChildItems, RESULT_GROUP_SMALL),
        mod0(counter.textAnchors(), RESULT_GROUP_LARGE),
        mod0(counter.textAnchors(), RESULT_GROUP_SMALL),
        distance(counter.textAnchors(), RESULT_GROUP_LARGE),
        distance(counter.textAnchors(), RESULT_GROUP_SMALL),

        Math.abs(children - counter.anchors()),
        Math.abs(childItems - counter.anchors()),
        evenlyDivisible(children, counter.anchors()),
        evenlyDivisible(childItems, counter.anchors()),
        smallestMod(children, counter.anchors()),
        smallestMod(childItems, counter.anchors()),

        Math.abs(children - counter.textAnchors()),
        Math.abs(childItems - counter.textAnchors()),
        Math.abs(children - anchorChildren),
        Math.abs(childItems - anchorChildItems),
        Math.abs(children - textChildren),
        Math.abs(childItems - textChildItems),
        Math.abs(children - anchorTextChildren),
        Math.abs(childItems - anchorTextChildItems),
        evenlyDivisible(children, counter.textAnchors()),
        evenlyDivisible(childItems, counter.textAnchors()),
        evenlyDivisible(children, anchorChildren),
        evenlyDivisible(childItems, anchorChildItems),
        evenlyDivisible(children, textChildren),
        evenlyDivisible(childItems, textChildItems),
        evenlyDivisible(children, anchorTextChildren),
        evenlyDivisible(childItems, anchorTextChildItems),
        smallestMod(children, counter.textAnchors()),
        smallestMod(childItems, counter.textAnchors()),
        smallestMod(children, anchorChildren),
        smallestMod(childItems, anchorChildItems),
        smallestMod(children, textChildren),
        smallestMod(childItems, textChildItems),
        smallestMod(children, anchorTextChildren),
        smallestMod(childItems, anchorTextChildItems),

        Math.abs(anchorChildren - anchorChildItems),
        Math.abs(textChildren - textChildItems),
        Math.abs(anchorTextChildren - anchorTextChildItems),

        toInt(statAnchorChars.getSkewness()),
        toInt(statAnchorChars.getStandardDeviation()),
        toInt(statAnchorChars.getMean()),
        toInt(statAnchors.getSkewness()),
        toInt(statAnchors.getStandardDeviation()),
        toInt(statAnchors.getMean()),
        toInt(statChars.getSkewness()),
        toInt(statChars.getStandardDeviation()),
        toInt(statChars.getMean()),
        toInt(statDescendants.getSkewness()),
        toInt(statDescendants.getStandardDeviation()),
        toInt(statDescendants.getMean()),
        toInt(statFields.getSkewness()),
        toInt(statFields.getStandardDeviation()),
        toInt(statFields.getMean()),
        toInt(statLevels.getSkewness()),
        toInt(statLevels.getStandardDeviation()),
        toInt(statLevels.getMean()),
        toInt(statLongestField.getSkewness()),
        toInt(statLongestField.getStandardDeviation()),
        toInt(statLongestField.getMean()),
        toInt(statNonAnchorChars.getSkewness()),
        toInt(statNonAnchorChars.getStandardDeviation()),
        toInt(statNonAnchorChars.getMean()),
        toInt(statStrLen.getSkewness()),
        toInt(statStrLen.getStandardDeviation()),
        toInt(statStrLen.getMean()),
        toInt(statTextAnchors.getSkewness()),
        toInt(statTextAnchors.getStandardDeviation()),
        toInt(statTextAnchors.getMean()),
        toInt(statItemChars.getSkewness()),
        toInt(statItemChars.getStandardDeviation()),
        toInt(statItemChars.getMean()),
        toInt(statItemAnchorChars.getSkewness()),
        toInt(statItemAnchorChars.getStandardDeviation()),
        toInt(statItemAnchorChars.getMean()),
    };
  }

  private static int div(int num, int denom) {
    return div((double) num, (double) denom);
  }

  private static int div(double num, double denom) {
    return toInt(Math.abs(denom) < FLOAT_ROUNDING_ERR ? 0d : num / denom);
  }

  private static int distance(int arg, int k) {
    int lower = arg / k;
    int upper = lower + 1;
    lower = lower == 0 ? 1 : lower;
    return Math.min(Math.abs(arg - (lower * k)), Math.abs(arg - (upper * k)));
  }

  private static int mod0(int arg, int k) {
    if (arg == 0) {
      return toInt(false);
    }
    return toInt(arg % k == 0);
  }

  private static int smallestMod(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    int ltr = a % b;
    int rtl = b % a;
    return Math.min(ltr, rtl);
  }

  private static int evenlyDivisible(int a, int b) {
    if (a == 0 || b == 0) {
      return toInt(false);
    }
    if (a % b == 0) {
      return toInt(true);
    }
    if (b % a == 0) {
      return toInt(true);
    }
    return toInt(false);
  }

  private static int toInt(boolean b) {
    return b ? -1 : 1;
  }

  private static int toInt(double d) {
    long tmp = (long) (d * FLOAT_SIGNIFICANT_DIGITS);
    if (tmp > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (tmp < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) tmp;
  }

  private static int compare(int lhs, int rhs, double tolerance) {
    if (lhs > 0 && rhs > 0
        && ((double) Math.abs(lhs - rhs)) / ((double) (lhs + rhs)) < tolerance) {
      return 0;
    }
    if (lhs < rhs) {
      return -1;
    }
    if (lhs > rhs) {
      return 1;
    }
    return 0;
  }

  public Node node() {
    return node;
  }

  /**
   * @return -1 if better than arg, 0 if equal, 1 if worse
   */
  public int compare(ComparableNode other) {
    int cur = 0;
    for (int i = 0; i < scores.length; i++) {
      NeuralNetManager.instance(thread).set(cur++,
          compare(scores[i], other.scores[i], SIGNIFICANT_DIFF));
    }
    int result = NeuralNetManager.instance(thread).pull();
    result = result < 0 ? -1 : result > 0 ? 1 : 0;
    return result;
  }
}
