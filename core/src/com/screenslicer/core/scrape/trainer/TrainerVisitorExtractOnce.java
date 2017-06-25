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
    Node winner = Extract.perform(elements.get(curTrainingData), 1, null, null, null, null, 0).get(0);
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
