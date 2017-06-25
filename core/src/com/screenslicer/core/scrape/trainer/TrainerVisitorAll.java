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

import com.screenslicer.api.request.KeywordQuery;
import com.screenslicer.core.scrape.ProcessPage;
import com.screenslicer.core.scrape.trainer.TrainerSimple.Visitor;
import com.screenslicer.core.scrape.type.ScrapeResult;
import com.screenslicer.core.util.NodeUtil;

public class TrainerVisitorAll implements Visitor {
  private final ArrayList<Element> elements = new ArrayList<Element>();
  private final ArrayList<Integer> nums = new ArrayList<Integer>();
  private String[] names;
  private static final boolean BUMP_ONLY = false;
  private KeywordQuery query = new KeywordQuery();

  @Override
  public void init() {
    query.keywords = "united states usa us scotland kobe amazon ibm family arroyo reds beanie kimono pants";
    final ArrayList<String> filenames = new ArrayList<String>();
    final List<String> bump = Arrays.asList(new String[] {
        });
    new File(System.getProperty("screenslicer.testdata")).listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        if (!file.getAbsolutePath().endsWith("-result")
            && !file.getAbsolutePath().endsWith("-success")
            && !file.getAbsolutePath().endsWith("-successnode")
            && !file.getAbsolutePath().endsWith("-num")
            && !file.getAbsolutePath().endsWith("-next")) {
          try {
            if (bump.contains(file.getName())) {
              File numFile = new File(file.getAbsolutePath() + "-num");
              if (numFile.exists()) {
                nums.add(0, Integer.parseInt(FileUtils.readFileToString(numFile, "utf-8")));
              } else {
                nums.add(0, 10);
              }
              elements.add(0, NodeUtil.markTestElement(DataUtil.load(file, "utf-8", "http://localhost").body()));
              filenames.add(0, file.getName());
            } else if (!BUMP_ONLY) {
              File numFile = new File(file.getAbsolutePath() + "-num");
              if (numFile.exists()) {
                nums.add(Integer.parseInt(FileUtils.readFileToString(numFile, "utf-8")));
              } else {
                nums.add(10);
              }
              elements.add(NodeUtil.markTestElement(DataUtil.load(file, "utf-8", "http://localhost").body()));
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
    List<ScrapeResult> processedResults = ProcessPage.perform(elements.get(curTrainingData), page, query, 0);
    if (processedResults == null) {
      System.out.println(">>>>>>>>>>  error - " + names[curTrainingData]);
      System.out.println("=====================================================================");
    } else {
      System.out.println(processedResults.size()
          + " / " + nums.get(curTrainingData)
          + " --- " + names[curTrainingData]);
      System.out.println(ProcessPage.infoString(processedResults));
      System.out.println("=====================================================================");
    }
    return 0;
  }

  @Override
  public int trainingDataSize() {
    return elements.size();
  }
}
