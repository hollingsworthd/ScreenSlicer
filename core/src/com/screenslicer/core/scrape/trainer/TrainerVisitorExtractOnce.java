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
package com.screenslicer.core.scrape.trainer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.screenslicer.core.scrape.Extract;

public class TrainerVisitorExtractOnce implements TrainerExtractOnce.Visitor {
  private final ArrayList<String> resultParents = new ArrayList<String>();
  private final ArrayList<Element> elements = new ArrayList<Element>();
  private String[] names;

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
              resultParents.add(0, FileUtils.readFileToString(new File(file.getAbsolutePath() + "-success"), "utf-8"));
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
    names = filenames.toArray(new String[0]);
  }

  @Override
  public int visit(int curTrainingData, int page) {
    long start = System.currentTimeMillis();
    Node winner = Extract.perform(elements.get(curTrainingData), 1, null, null, null, null).get(0);
    long dur = System.currentTimeMillis() - start;
    if (winner == null
        || !winner.outerHtml().startsWith(resultParents.get(curTrainingData))) {
      System.out.println("Fail: " + names[curTrainingData] + (winner == null ? ", null" : ""));
    } else {
      System.out.println(dur);
    }
    return -1;
  }

  @Override
  public int trainingDataSize() {
    return resultParents.size();
  }
}
