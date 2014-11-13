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

import com.screenslicer.api.datatype.SearchResult;
import com.screenslicer.api.request.Query;

public class SearchResults {
  private List<SearchResult> searchResults;
  private List<SearchResult> prevResults;
  private static Collection<SearchResults> instances = new HashSet<SearchResults>();
  private boolean isValid = true;
  private static Object lock = new Object();
  private Query query;
  private int page;

  public static SearchResults newInstance(
      List<SearchResult> searchResults, int page, Query query) {
    SearchResults instance = new SearchResults(searchResults, page, query);
    synchronized (lock) {
      instances.add(instance);
    }
    return instance;
  }

  public static SearchResults newInstance() {
    SearchResults instance = new SearchResults();
    synchronized (lock) {
      instances.add(instance);
    }
    return instance;
  }

  public static void invalidate() {
    synchronized (lock) {
      for (SearchResults cur : instances) {
        cur.isValid = false;
      }
    }
  }

  private SearchResults(List<SearchResult> searchResults, int page, Query query) {
    this.searchResults = searchResults;
    this.prevResults = new ArrayList<SearchResult>(searchResults);
    this.page = page;
    this.query = query;
  }

  private SearchResults() {
    this.searchResults = new ArrayList<SearchResult>();
    this.prevResults = new ArrayList<SearchResult>();
    this.page = -1;
    this.query = null;
  }

  public boolean isEmpty() {
    return searchResults.isEmpty();
  }

  public int size() {
    return searchResults.size();
  }

  public void remove(int index) {
    if (index < searchResults.size()) {
      searchResults.remove(index);
    }
  }

  public List<SearchResult> deregister() {
    synchronized (lock) {
      instances.remove(this);
    }
    return searchResults;
  }

  public void addPage(SearchResults newResults) {
    List<SearchResult> results = newResults.deregister();
    searchResults.addAll(results);
    this.prevResults = new ArrayList<SearchResult>(results);
    this.page = newResults.page;
    this.query = newResults.query;
  }

  public void remoteLastPage() {
    for (SearchResult toRemove : prevResults) {
      searchResults.remove(toRemove);
    }
    prevResults.clear();
  }

  public SearchResult get(int index) {
    if (index >= searchResults.size()) {
      return new SearchResult();
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
