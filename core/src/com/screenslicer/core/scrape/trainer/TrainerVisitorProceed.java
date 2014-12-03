/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2014 Machine Publishers, LLC
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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.Proceed;

public class TrainerVisitorProceed implements TrainerProceed.Visitor {
  private final ArrayList<String> nextButtons = new ArrayList<String>();
  private final ArrayList<Element> elements = new ArrayList<Element>();
  private String[] names;

  @Override
  public void init() {
    final ArrayList<String> filenames = new ArrayList<String>();
    final List<String> bump = Arrays.asList(new String[] {
        "buzzfeed"
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
            File fileNext = new File(file.getAbsolutePath() + "-next");
            if (fileNext.exists()) {
              if (bump.contains(file.getName())) {
                nextButtons.add(0, FileUtils.readFileToString(fileNext, "utf-8"));
                elements.add(0, DataUtil.load(file, "utf-8", "http://localhost").body());
                filenames.add(0, file.getName());
              } else {
                nextButtons.add(FileUtils.readFileToString(fileNext, "utf-8"));
                elements.add(DataUtil.load(file, "utf-8", "http://localhost").body());
                filenames.add(file.getName());
              }
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
  public int visit(int curTrainingData) {
    int result = 0;
    if (!nextButtons.get(curTrainingData).equals("unknown")) {
      Node next = Proceed.perform(elements.get(curTrainingData), 2).node;
      if (next == null && nextButtons.get(curTrainingData).equals("n/a")) {
        System.out.println("pass - " + names[curTrainingData]);
      } else if (next != null
          && CommonUtil.strip(next.outerHtml(), false).replace(" ", "")
              .startsWith(CommonUtil.strip(nextButtons.get(curTrainingData), false).replace(" ", ""))) {
        System.out.println("pass - " + names[curTrainingData]);
      } else {
        System.out.println("fail - " + names[curTrainingData]);
        if (next != null) {
          System.out.println("Actual--" + CommonUtil.strip(next.outerHtml(), false));
        }
        System.out.println("Expected--" + CommonUtil.strip(nextButtons.get(curTrainingData), false));
        result = 1;
      }
    }
    return result;
  }

  @Override
  public int trainingDataSize() {
    return nextButtons.size();
  }
}
