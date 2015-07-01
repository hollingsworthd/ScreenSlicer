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
package com.screenslicer.api.request;

import com.screenslicer.api.datatype.Credentials;
import com.screenslicer.api.datatype.HtmlNode;
import com.screenslicer.api.datatype.UrlTransform;

public abstract class Query {
  /**
   * Whether to return only a unique ID for each SearchResult which can later be
   * used to request the actual content. Useful for very large result sets.
   */
  public boolean collapse;
  /**
   * URL of search page
   */
  public String site;
  /**
   * Substrings that URLs of results must contain
   */
  public String[] urlWhitelist;
  /**
   * Regular expressions that URLs of results must match
   */
  public String[] urlPatterns;
  /**
   * HtmlNodes that result URL nodes must match;
   */
  public HtmlNode[] urlMatchNodes;
  /**
   * Whether to apply the urlWhitelist and urlPatterns before analyzing
   * the page to extract results. This generally produces a more accurate
   * extraction.
   */
  public boolean proactiveUrlFiltering;
  /**
   * Converts result URLs to another format, based on regular expressions.
   */
  public UrlTransform[] urlTransforms;
  /**
   * Maximum number of search pages to extract, unless the
   * results maximum has already been reached.
   * Defaults to 1. Set to 0 or less to disable this maximum.
   */
  public int pages = 1;
  /**
   * Maximum number of results to extract, unless the
   * pages maximum has already been reached.
   * Defaults to 0. Set to 0 or less to disable this maximum.
   */
  public int results = 0;
  /**
   * Whether to get the content at each result URL
   */
  public boolean fetch;
  /**
   * Whether to visit the result URL directly or try a public web cache
   */
  public boolean fetchCached;
  /**
   * Whether to fetch results in a new window.
   */
  public boolean fetchInNewWindow = true;
  /**
   * Whether to extract results or just return all the HTML
   */
  public boolean extract = true;
  /**
   * Override to specify a particular type of node that's a parent of result
   * nodes to extract.
   */
  public HtmlNode matchParent;
  /**
   * Override to specify a particular type of node that's a result node to
   * extract.
   */
  public HtmlNode matchResult;
  /**
   * Whether results must have anchors
   */
  public boolean requireResultAnchor = true;
  /**
   * Click to submit search.
   */
  public HtmlNode searchSubmitClick;
  /**
   * Clicks on HTML elements prior to authentication
   */
  public HtmlNode[] preAuthClicks;
  /**
   * Clicks on HTML elements prior to searching
   */
  public HtmlNode[] preSearchClicks;
  /**
   * Clicks on HTML elements after searching
   */
  public HtmlNode[] postSearchClicks;
  /**
   * Clicks on HTML elements to get successive pages of results
   */
  public HtmlNode[] proceedClicks;
  /**
   * Clicks on HTML elements at a result page after fetching it
   */
  public HtmlNode[] postFetchClicks;
  /**
   * Attach media to the result which match these HtmlNodes
   */
  public HtmlNode[] media;
  /**
   * Whether to attach all media.
   */
  public boolean allMedia;
  /**
   * Credentials for authentication
   */
  public Credentials credentials;
  /**
   * Whether to throttle requests
   */
  public boolean throttle = true;
  /**
   * KeywordQuery to perform at each fetched result.
   * Do not set a value for both keywordQuery and formQuery (one of them or both must be null).
   */
  public KeywordQuery keywordQuery;
  /**
   * FormQuery to perform at each fetched result.
   * Do not set a value for both keywordQuery and formQuery (one of them or both must be null).
   */
  public FormQuery formQuery;

  private transient Progress progress = new Progress();

  public void markResult(int cur) {
    progress.markResult(cur);
  }

  public void markPage(int cur) {
    progress.markPage(cur);
  }

  public int currentResult() {
    return progress.currentResult();
  }

  public int currentPage() {
    return progress.currentPage();
  }

  public abstract boolean isKeywordQuery();

  public abstract boolean isFormQuery();
}
