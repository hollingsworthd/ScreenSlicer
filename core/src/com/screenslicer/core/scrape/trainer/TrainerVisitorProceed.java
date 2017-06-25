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
    new File(System.getProperty("screenslicer.testdata")).listFiles(new FileFilter() {
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
