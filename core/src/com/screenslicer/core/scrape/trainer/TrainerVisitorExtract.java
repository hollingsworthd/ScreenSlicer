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
    new File(System.getProperty("screenslicer.testdata")).listFiles(new FileFilter() {
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
          Extract.trainInit(elements.get(curItem), page, 0);
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
      int curScore = Extract.train(elements.get(curItem), page, targetsArray[i],
          indices.get(targetsArray[i]), 0);
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
