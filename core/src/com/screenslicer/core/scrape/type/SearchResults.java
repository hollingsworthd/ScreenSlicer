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
import java.util.List;

import com.screenslicer.api.datatype.SearchResult;
import com.screenslicer.api.request.Query;

public class SearchResults {
  private List<SearchResult> searchResults;
  private List<SearchResult> prevResults;
  private Query query;
  private int page;

  public SearchResults(List<SearchResult> searchResults, int page, Query query) {
    this.searchResults = searchResults;
    this.prevResults = new ArrayList<SearchResult>(searchResults);
    this.page = page;
    this.query = query;
  }

  public SearchResults() {
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

  public List<SearchResult> list() {
    return searchResults;
  }

  public void addPage(SearchResults newResults) {
    searchResults.addAll(newResults.list());
    this.prevResults = new ArrayList<SearchResult>(newResults.list());
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
