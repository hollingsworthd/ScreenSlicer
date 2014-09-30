/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * 717 Martin Luther King Dr W Ste I, Cincinnati, Ohio 45220
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--contact Machine Publishers, LLC for details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
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
import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import com.screenslicer.core.util.Util;

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

  public NodeCounter(Node node) {
    this(Arrays.asList(new Node[] { node }));
  }

  public NodeCounter(List<Node> nodes) {
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
          if (!Util.isEmpty(node)) {
            count++;
            if (node.nodeName().equals("#text")) {
              int curLen = Util.trimmedLen(node.toString());
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
            if (Util.isBlock(node.nodeName())) {
              blocks++;
            }
            if (Util.isFormatting(node.nodeName())) {
              formatting++;
            }
            if (Util.isDecoration(node.nodeName())) {
              decoration++;
            }
            if (Util.isContent(node)) {
              content++;
            }
            if (Util.isItem(node.nodeName())) {
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
