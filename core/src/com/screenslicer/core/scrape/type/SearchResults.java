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
package com.screenslicer.core.scrape.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.openqa.selenium.remote.BrowserDriver;
import org.openqa.selenium.remote.BrowserDriver.Fatal;

import com.screenslicer.api.datatype.Result;
import com.screenslicer.api.request.Query;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.core.scrape.ProcessPage;
import com.screenslicer.core.scrape.Scrape.ActionFailed;

public class SearchResults {
  private List<Result> searchResults;
  private List<Result> prevResults;
  private static Collection<SearchResults> instances = new HashSet<SearchResults>();
  private static Object lock = new Object();
  private String window;
  private int page;
  private Query query;
  private static final double MAX_ERROR = .15;

  public static SearchResults newInstance(boolean register) {
    return newInstance(register, null, null, -1, null);
  }

  public static SearchResults newInstance(boolean register,
      List<Result> searchResults, SearchResults source) {
    if (source != null) {
      synchronized (lock) {
        instances.remove(source);
      }
      return newInstance(register, searchResults, source.window, source.page, source.query);
    }
    return newInstance(register, searchResults, null, -1, null);
  }

  public static SearchResults newInstance(boolean register,
      List<Result> searchResults, String window, int page, Query query) {
    SearchResults instance = new SearchResults(searchResults, window, page, query);
    if (register) {
      synchronized (lock) {
        instances.add(instance);
      }
    }
    return instance;
  }

  private SearchResults(List<Result> searchResults, String window, int page, Query query) {
    this.searchResults = searchResults == null ? new ArrayList<Result>()
        : new ArrayList<Result>(searchResults);
    this.prevResults = searchResults == null ? new ArrayList<Result>()
        : new ArrayList<Result>(searchResults);
    this.window = window;
    this.page = page;
    this.query = query;
  }

  public static void revalidate(BrowserDriver driver, boolean reset) {
    Collection<SearchResults> myInstances;
    synchronized (lock) {
      myInstances = new HashSet<SearchResults>(instances);
    }
    if (reset) {
      driver.reset();
    }
    for (SearchResults cur : myInstances) {
      if (cur.window != null && cur.query != null && !CommonUtil.isEmpty(cur.prevResults)) {
        List<Result> prevPage = cur.removeLastPage();
        try {
          driver.switchTo().window(cur.window);
          driver.switchTo().defaultContent();
          List<Result> newPage = new ArrayList<Result>(ProcessPage.perform(driver, cur.page, cur.query).drain());
          for (int num = newPage.size(); num > prevPage.size(); num--) {
            newPage.remove(num - 1);
          }
          double diff = Math.abs(newPage.size() - prevPage.size());
          if (diff / (double) prevPage.size() > MAX_ERROR) {
            throw new Fatal();
          }
          cur.prevResults = newPage;
          cur.window = driver.getWindowHandle();
          cur.searchResults.addAll(cur.prevResults);
        } catch (Fatal f) {
          cur.prevResults = prevPage;
          cur.searchResults.addAll(prevPage);
          throw new Fatal(f);
        } catch (ActionFailed e) {
          cur.prevResults = prevPage;
          cur.searchResults.addAll(prevPage);
          throw new Fatal(e);
        }
      }
    }
    String[] handles = driver.getWindowHandles().toArray(new String[0]);
    driver.switchTo().window(handles[handles.length - 1]);
    driver.switchTo().defaultContent();
  }

  private List<Result> removeLastPage() {
    if (!CommonUtil.isEmpty(prevResults)) {
      for (Result toRemove : prevResults) {
        searchResults.remove(toRemove);
      }
      List<Result> lastPage = new ArrayList<Result>(prevResults);
      prevResults.clear();
      return lastPage;
    }
    return new ArrayList<Result>();
  }

  public boolean isEmpty() {
    return CommonUtil.isEmpty(searchResults);
  }

  public int size() {
    return searchResults == null ? 0 : searchResults.size();
  }

  public void remove(int index) {
    if (searchResults != null) {
      if (index < searchResults.size()) {
        Result removed = searchResults.remove(index);
        if (removed != null) {
          prevResults.remove(removed);
        }
      }
    }
  }

  public List<Result> drain() {
    synchronized (lock) {
      instances.remove(this);
    }
    if (prevResults != null) {
      prevResults.clear();
    }
    return searchResults == null ? new ArrayList<Result>() : searchResults;
  }

  public boolean isDuplicatePage(SearchResults newResults) {
    int diff = 0;
    if (newResults != null && prevResults != null && newResults.searchResults != null) {
      if (!prevResults.isEmpty() && prevResults.size() == newResults.searchResults.size()) {
        for (int i = 0; i < prevResults.size(); i++) {
          if (!CommonUtil.equals(prevResults.get(i).summary, newResults.searchResults.get(i).summary)
              || !CommonUtil.equals(prevResults.get(i).title, newResults.searchResults.get(i).title)
              || !CommonUtil.equals(prevResults.get(i).url, newResults.searchResults.get(i).url)) {
            ++diff;
          }
        }
        return (double) diff / (double) prevResults.size() <= MAX_ERROR;
      }
    }
    if (newResults == null || newResults.isEmpty()) {
      return true;
    }
    return false;
  }

  public void addPage(SearchResults newResults) {
    if (newResults != null) {
      if (searchResults == null) {
        searchResults = new ArrayList<Result>();
      }
      List<Result> results = newResults.drain();
      searchResults.addAll(results);
      this.prevResults = new ArrayList<Result>(results);
      this.window = newResults.window;
      this.page = newResults.page;
      this.query = newResults.query;
    }
  }

  public Result get(int index) {
    if (searchResults == null || index >= searchResults.size()) {
      return new Result();
    }
    return searchResults.get(index);
  }

  public Query query() {
    return query;
  }

  public int page() {
    return page;
  }
}
