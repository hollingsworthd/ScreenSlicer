/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * ScreenSlicer is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking ScreenSlicer statically or dynamically with other modules is making a combined work
 *   based on ScreenSlicer. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of ScreenSlicer with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on ScreenSlicer. If you modify ScreenSlicer, you may not
 *   extend this exception to your modified version of ScreenSlicer.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
 */
package com.screenslicer.core.scrape.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.core.util.NodeUtil;

public class NodeCounter {
  private int count = 0;
  private int levels = 0;
  private int fields = 0;
  private int images = 0;
  private int anchors = 0;
  private int textAnchors = 0;
  private int anchorLen = 0;
  private int chars = 0;
  private int nonAnchorChars = 0;
  private int anchorChars = 0;
  private int blocks = 0;
  private int content = 0;
  private int items = 0;
  private int formatting = 0;
  private int decoration = 0;
  private int longestField = 0;
  private List<String> tags = new ArrayList<String>();
  private List<String> orderedTags = new ArrayList<String>();
  private boolean tagsSorted = false;
  private int insideAnchor = 0;

  public NodeCounter(Node node, final HtmlNode matchResult, final HtmlNode matchParent) {
    this(Arrays.asList(new Node[] { node }), matchResult, matchParent);
  }

  public NodeCounter(List<Node> nodes, final HtmlNode matchResult, final HtmlNode matchParent) {
    for (Node cur : nodes) {
      if (cur == null) {
        continue;
      }
      cur.traverse(new NodeVisitor() {
        @Override
        public void tail(Node node, int depth) {
          if (node.nodeName().equals("a")) {
            insideAnchor--;
          }
        }

        @Override
        public void head(Node node, int depth) {
          if (!NodeUtil.isEmpty(node)) {
            count++;
            if (node.nodeName().equals("#text")) {
              int curLen = NodeUtil.trimmedLen(node.toString());
              if (curLen > 0) {
                fields++;
              }
              chars += curLen;
              longestField = Math.max(longestField, curLen);
              if (insideAnchor == 0) {
                nonAnchorChars += curLen;
              } else {
                textAnchors++;
                anchorChars += curLen;
              }
            }
            if (node.nodeName().equals("img")) {
              images++;
            }
            if (node.nodeName().equals("a")) {
              insideAnchor++;
              anchors++;
              anchorLen += node.attr("abs:href").length();
            }
            if (NodeUtil.isBlock(node.nodeName())) {
              blocks++;
            }
            if (NodeUtil.isFormatting(node.nodeName())) {
              formatting++;
            }
            if (NodeUtil.isDecoration(node.nodeName())) {
              decoration++;
            }
            if (NodeUtil.isContent(node, matchResult, matchParent)) {
              content++;
            }
            if (NodeUtil.isItem(node, matchResult, matchParent)) {
              items++;
            }
            if (!tags.contains(node.nodeName())) {
              tags.add(node.nodeName());
            }
            orderedTags.add(node.nodeName());
          }
          levels = Math.max(levels, depth);
        }
      });
    };
  }

  public int descendants() {
    return count;
  }

  public int levels() {
    return levels;
  }

  public int fields() {
    return fields;
  }

  public int images() {
    return images;
  }

  public int anchors() {
    return anchors;
  }

  public int anchorLen() {
    return (int) ((float) anchorLen / (float) anchors);
  }

  public int blocks() {
    return blocks;
  }

  public int formatting() {
    return formatting;
  }

  public int decoration() {
    return decoration;
  }

  public int content() {
    return content;
  }

  public int items() {
    return items;
  }

  public int chars() {
    return chars;
  }

  public int textAnchors() {
    return textAnchors;
  }

  public int anchorChars() {
    return anchorChars;
  }

  public int nonAnchorChars() {
    return nonAnchorChars;
  }

  public int longestField() {
    return longestField;
  }

  public List<String> tags() {
    if (!tagsSorted) {
      String[] tagsArray = tags.toArray(new String[0]);
      Arrays.sort(tagsArray);
      tags = Arrays.asList(tagsArray);
      tagsSorted = true;
    }
    return tags;
  }

  public List<String> orderedTags() {
    return orderedTags;
  }
}
