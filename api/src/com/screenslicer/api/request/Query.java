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
