/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
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
