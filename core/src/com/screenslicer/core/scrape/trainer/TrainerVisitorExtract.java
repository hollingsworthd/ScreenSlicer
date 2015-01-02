/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
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
package com.screenslicer.core.scrape.trainer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Element;

import com.screenslicer.core.scrape.Extract;
import com.screenslicer.core.scrape.type.ComparableNode;

public class TrainerVisitorExtract implements TrainerExtract.Visitor {
  private ArrayList<String> resultParents = new ArrayList<String>();
  private ArrayList<Element> elements = new ArrayList<Element>();
  private final Map<Element, ComparableNode[]> nodes = new HashMap<Element, ComparableNode[]>();
  private Object[] tmpItems;
  private int curItem;
  private static final ItemSort sorter = new ItemSort();
  private Map<ComparableNode, Integer> indices = new HashMap<ComparableNode, Integer>();

  @Override
  public void init() {
    final ArrayList<String> filenames = new ArrayList<String>();
    final List<String> bump = Arrays.asList(new String[] {
        });
    new File("./test/external/").listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        if (!file.getAbsolutePath().endsWith("-success")
            && !file.getAbsolutePath().endsWith("-successnode")
            && !file.getAbsolutePath().endsWith("-result")
            && !file.getAbsolutePath().endsWith("-num")
            && !file.getAbsolutePath().endsWith("-next")) {
          try {
            if (bump.contains(file.getName())) {
              resultParents.add(0, FileUtils.readFileToString(new File(file.getAbsolutePath() + "-success", "utf-8")));
              elements.add(0, DataUtil.load(file, "utf-8", "http://localhost").body());
              filenames.add(0, file.getName());
            } else {
              resultParents.add(FileUtils.readFileToString(new File(file.getAbsolutePath() + "-success", "utf-8")));
              elements.add(DataUtil.load(file, "utf-8", "http://localhost").body());
              filenames.add(file.getName());
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        return false;
      }
    });
    for (String filename : filenames) {
      System.out.println(filename);
    }
    tmpItems = new Object[resultParents.size()];
  }

  @Override
  public void reload(boolean sort) {
    if (sort) {
      for (int i = curItem; i < tmpItems.length; i++) {
        tmpItems[i] = new Object[] {
            0,
            elements.get(i),
            resultParents.get(i) };
      }
      Arrays.sort(tmpItems, sorter);
      elements.clear();
      resultParents.clear();
      for (int i = 0; i < tmpItems.length; i++) {
        elements.add((Element) ((Object[]) tmpItems[i])[1]);
        resultParents.add((String) ((Object[]) tmpItems[i])[2]);
      }
    }
    curItem = 0;
  }

  private static class ItemSort implements Comparator<Object> {
    @Override
    public int compare(Object o1, Object o2) {
      Integer lhs = (Integer) ((Object[]) o1)[0];
      Integer rhs = (Integer) ((Object[]) o2)[0];
      return rhs.compareTo(lhs);
    }
  }

  @Override
  public int visitNext(int page) {
    ComparableNode[] targetsArray = nodes.get(elements.get(curItem));
    if (targetsArray == null) {
      ComparableNode[] comparableNodes =
          Extract.trainInit(elements.get(curItem), page);
      List<ComparableNode> targets = new ArrayList<ComparableNode>();
      for (int i = 0; i < comparableNodes.length; i++) {
        if (comparableNodes[i].node().outerHtml().startsWith(
            resultParents.get(curItem))) {
          targets.add(comparableNodes[i]);
        }
      }
      targetsArray = targets.toArray(new ComparableNode[0]);
      for (int i = 0; i < comparableNodes.length; i++) {
        for (int j = 0; j < targetsArray.length; j++) {
          if (comparableNodes[i].equals(targetsArray[j])) {
            indices.put(targetsArray[j], i);
          }
        }
      }
      nodes.put(elements.get(curItem), targetsArray);
    }
    int score = Integer.MAX_VALUE;
    for (int i = 0; i < targetsArray.length && score != 0; i++) {
      int curScore = Extract.train(elements.get(curItem), page, targetsArray[i], indices.get(targetsArray[i]));
      if (curScore < score) {
        score = curScore;
      }
    }
    tmpItems[curItem] = new Object[] {
        score,
        elements.get(curItem),
        resultParents.get(curItem) };
    ++curItem;
    return score;
  }

  @Override
  public int trainingDataSize() {
    return resultParents.size();
  }
}
