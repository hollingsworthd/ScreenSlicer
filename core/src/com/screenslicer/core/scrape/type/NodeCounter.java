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
