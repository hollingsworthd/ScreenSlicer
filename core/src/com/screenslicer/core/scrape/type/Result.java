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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Node;
import org.reflections.util.Utils;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.screenslicer.common.CommonUtil;
import com.screenslicer.common.Log;
import com.screenslicer.core.util.Util;

public class Result {
  static {
    if (new Parser() != null) {
      LogManager.getLogManager().getLogger("com.joestelmach.natty").setLevel(Level.OFF);
    }
  }
  private static final int MAX_TOKENS = 2;
  private static final double DATE_LEN_THRESHOLD = .25d;
  private List<Node> myNodes = new ArrayList<Node>();
  private boolean summaryBeforeTitle = false;
  private String url = null;
  private String urlTitle = null;
  private String priorUrl = null;
  private String priorUrlTitle = null;
  private List<String> fallbackUrls = new ArrayList<String>();
  private List<String> fallbackUrlTitles = new ArrayList<String>();
  private String altUrl = null;
  private String altUrlTitle = null;
  private boolean hasImgUrl = false;
  private Map<String, Node> urlNodes = new HashMap<String, Node>();
  private SummaryNodes summary = new SummaryNodes();
  private String dateAnyFormat = null;
  private String dateDDMMYYYYFormat = null;
  private static DateFormat dateTimeDDMMYYYY = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
  private static DateFormat dateDDMMYYYY = new SimpleDateFormat("dd/MM/yyyy");
  private static Pattern dateToken = Pattern.compile("([0-9][-\\s0-9/,:]+[0-9])", Pattern.UNICODE_CHARACTER_CLASS);
  private static Pattern likelyDate = Pattern.compile("\\d");
  private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  private static final Pattern titleJunk = Pattern.compile("^(?:permanent\\slink\\sto\\s|permalink\\sto\\s)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  private boolean textFlagged = false;
  private boolean isDDMMYYYY = false;
  private boolean useDDMMYYYY = false;
  private int numUrls = 0;
  private boolean titleHasTextSibling = false;
  private boolean titleHasAnchorSibling = false;
  private boolean titleHasLoneBlock = false;
  private boolean titleHasImage = false;
  private String attachment = null;
  static {
    Logger.getLogger("com.joestelmach.natty").setLevel(Level.OFF);
    dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateDDMMYYYY.setLenient(false);
    dateTimeDDMMYYYY.setLenient(false);
  }

  public Result copy() {
    Result copy = new Result();
    copy.myNodes = new ArrayList<Node>(this.myNodes);
    copy.summaryBeforeTitle = this.summaryBeforeTitle;
    copy.url = this.url;
    copy.urlTitle = this.urlTitle;
    copy.priorUrl = this.priorUrl;
    copy.priorUrlTitle = this.priorUrlTitle;
    copy.fallbackUrls = new ArrayList<String>(this.fallbackUrls);
    copy.fallbackUrlTitles = new ArrayList<String>(this.fallbackUrlTitles);
    copy.altUrl = this.altUrl;
    copy.altUrlTitle = this.altUrlTitle;
    copy.hasImgUrl = this.hasImgUrl;
    copy.urlNodes = new HashMap(this.urlNodes);
    copy.summary = this.summary.copy();
    copy.dateAnyFormat = this.dateAnyFormat;
    copy.dateDDMMYYYYFormat = this.dateDDMMYYYYFormat;
    copy.textFlagged = this.textFlagged;
    copy.isDDMMYYYY = this.isDDMMYYYY;
    copy.useDDMMYYYY = this.useDDMMYYYY;
    copy.numUrls = this.numUrls;
    copy.titleHasTextSibling = this.titleHasTextSibling;
    copy.titleHasAnchorSibling = this.titleHasAnchorSibling;
    copy.titleHasLoneBlock = this.titleHasLoneBlock;
    copy.titleHasImage = this.titleHasImage;
    copy.attachment = this.attachment;
    return copy;
  }

  public void attach(String attachment) {
    this.attachment = attachment;
  }

  public String attachment() {
    return this.attachment;
  }

  public void addFirst(Node node) {
    myNodes.add(0, node);
  }

  public void addLast(Node node) {
    myNodes.add(node);
  }

  public List<Node> getNodes() {
    return myNodes;
  }

  public boolean hasAllFields(boolean requireDate) {
    return !CommonUtil.isEmpty(title())
        && !CommonUtil.isEmpty(summary())
        && !CommonUtil.isEmpty(url())
        && (!requireDate || !CommonUtil.isEmpty(date()));
  }

  public void swapTitleAndSummary() {
    summary.cached = false;
    String curTitle = title();
    Node urlNode = urlNode();
    if (curTitle != null && priorUrlTitle != null && priorUrl != null) {
      if (curTitle.equals(altUrlTitle) && url().equals(altUrl)) {
        altUrlTitle = priorUrlTitle;
        altUrl = priorUrl;
        String summaryStr = summary.asString(urlNode(), altUrlTitle);
        summary.override(curTitle, urlNode);
        altUrlTitle = summaryStr;
      } else if (curTitle.equals(urlTitle) && url().equals(url)) {
        urlTitle = priorUrlTitle;
        url = priorUrl;
        String summaryStr = summary.asString(urlNode(), urlTitle);
        summary.override(curTitle, urlNode);
        urlTitle = summaryStr;
      }
    }
  }

  public int numUrls() {
    return numUrls;
  }

  public boolean isEmpty(boolean requireResultAnchor) {
    return (requireResultAnchor && CommonUtil.isEmpty(url()))
        || (CommonUtil.isEmpty(title()) && CommonUtil.isEmpty(summary()));
  }

  public void tweakUrl(String newUrl) {
    summary.cached = false;
    Node node = urlNodes.get(url());
    urlNodes.put(newUrl, node);
    this.url = newUrl;
    this.altUrl = newUrl;
  }

  public Node urlNode() {
    Node node = urlNodes.get(url());
    if (node == null && !this.myNodes.isEmpty()) {
      return this.myNodes.get(0);
    }
    return node;
  }

  public String url() {
    if (CommonUtil.isEmpty(url) && !CommonUtil.isEmpty(altUrl)) {
      return altUrl;
    }
    if (!Utils.isEmpty(urlTitle) && !CommonUtil.isEmpty(altUrlTitle)
        && urlTitle.length() < altUrlTitle.length()
        && !isImg(altUrl) && !isImg(altUrlTitle)) {
      return altUrl;
    }
    if (CommonUtil.isEmpty(url)) {
      return null;
    }
    return url;
  }

  public String title() {
    if (CommonUtil.isEmpty(urlTitle) && !CommonUtil.isEmpty(altUrlTitle)) {
      return altUrlTitle;
    }
    if (!Utils.isEmpty(urlTitle) && !CommonUtil.isEmpty(altUrlTitle)
        && urlTitle.length() < altUrlTitle.length()
        && !isImg(altUrl) && !isImg(altUrlTitle)) {
      return altUrlTitle;
    }
    if (CommonUtil.isEmpty(urlTitle)) {
      return null;
    }
    return urlTitle;
  }

  public String altFallbackTitle(int index) {
    if (index > -1 && index < fallbackUrlTitles.size()) {
      return fallbackUrlTitles.get(index);
    }
    return "";
  }

  public int altFallbackTitleCount() {
    return fallbackUrlTitles.size();
  }

  public boolean isAltUrlAndTitle() {
    return !CommonUtil.isEmpty(url()) && url().equals(altUrl) && title().equals(altUrlTitle);
  }

  public void useAltFallbackUrlAndTitle(int fallbackTitleIndex) {
    summary.cached = false;
    String tmpFallbackUrl = null;
    String tmpFallbackUrlTitle = null;
    if (fallbackTitleIndex >= fallbackUrls.size() || fallbackTitleIndex >= fallbackUrlTitles.size()) {
      int max = 0;
      String maxUrl = null;
      String maxTitle = null;
      for (int i = 0; i < fallbackUrlTitles.size(); i++) {
        if (fallbackUrlTitles.get(i).length() > max) {
          if (i < fallbackUrls.size()) {
            max = fallbackUrlTitles.get(i).length();
            maxUrl = fallbackUrls.get(i);
            maxTitle = fallbackUrlTitles.get(i);
          }
        }
      }
      if ((!CommonUtil.isEmpty(urlTitle) && urlTitle.length() > max)
          || (!CommonUtil.isEmpty(altUrlTitle) && altUrlTitle.length() > max)) {
        max = 0;
      }
      if (max > 0 && !CommonUtil.isEmpty(maxUrl) && !CommonUtil.isEmpty(maxTitle)) {
        tmpFallbackUrl = maxUrl;
        tmpFallbackUrlTitle = maxTitle;
      }
    } else if (fallbackTitleIndex > -1) {
      tmpFallbackUrl = fallbackUrls.get(fallbackTitleIndex);
      tmpFallbackUrlTitle = fallbackUrlTitles.get(fallbackTitleIndex);
    } else {
      tmpFallbackUrl = altUrl;
      tmpFallbackUrlTitle = altUrlTitle;
    }
    if (!CommonUtil.isEmpty(tmpFallbackUrl) && !CommonUtil.isEmpty(tmpFallbackUrlTitle)) {
      url = tmpFallbackUrl;
      urlTitle = tmpFallbackUrlTitle;
      altUrl = tmpFallbackUrl;
      altUrlTitle = tmpFallbackUrlTitle;
    }
  }

  public void useAltUrlAndTitle(boolean use) {
    summary.cached = false;
    if (use && !CommonUtil.isEmpty(altUrl) && !CommonUtil.isEmpty(altUrlTitle)) {
      url = altUrl;
      urlTitle = altUrlTitle;
    } else if (!use && !CommonUtil.isEmpty(url) && !CommonUtil.isEmpty(urlTitle)) {
      altUrl = url;
      altUrlTitle = urlTitle;
    }
  }

  public static class SummaryNodes {
    private Map<Node, String> strings = new LinkedHashMap<Node, String>();
    private boolean cached = false;
    private String cachedSummary = null;
    private boolean sticky = false;

    public SummaryNodes copy() {
      SummaryNodes copy = new SummaryNodes();
      copy.strings = new LinkedHashMap<Node, String>(this.strings);
      copy.cached = this.cached;
      copy.cachedSummary = this.cachedSummary;
      copy.sticky = this.sticky;
      return copy;
    }

    private void add(String str, Node node) {
      if (node == null || !strings.containsKey(node)) {
        strings.put(node, str);
        cached = false;
        sticky = false;
      }
    }

    private boolean contains(Node node) {
      return strings.containsKey(node);
    }

    private String asString(Node urlNode, String urlTitle) {
      if (cached || sticky) {
        return cachedSummary;
      }
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Node, String> entry : strings.entrySet()) {
        Node parent = entry.getKey().parent();
        if (!urlNode.equals(entry.getKey())
            && (!entry.getKey().nodeName().equals("a")
            || !entry.getKey().attr("href").equals(urlNode.attr("href")))
            && (!entry.getKey().nodeName().equals("img")
            || (!entry.getKey().attr("title").trim().equals(urlTitle)
                && !entry.getKey().attr("alt").trim().equals(urlTitle)
                && (parent == null
                || !parent.nodeName().equals("a")
                || !parent.attr("href").equals(urlNode.attr("href")))))) {
          builder.append(' ');
          builder.append(entry.getValue());
          builder.append(' ');
        }
      }
      cachedSummary = StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeXml(CommonUtil.strip(builder.toString().replaceAll("\\s+,", ",").trim(), true)));
      cached = true;
      return cachedSummary;
    }

    private void override(String summary, Node node) {
      cachedSummary = summary;
      cached = true;
      sticky = true;
      if (node != null) {
        strings.clear();
        strings.put(node, summary);
      }
    }

    private void merge(SummaryNodes summaryNodes) {
      for (Map.Entry<Node, String> entry : summaryNodes.strings.entrySet()) {
        add(entry.getValue(), entry.getKey());
        cached = false;
        sticky = false;
      }
    }
  }

  public SummaryNodes summaryNodes() {
    return this.summary;
  }

  public String summary() {
    return summary.asString(urlNode(), title());
  }

  public void summaryMerge(SummaryNodes summaryNodes) {
    this.summary.merge(summaryNodes);
  }

  public String date() {
    if (useDDMMYYYY) {
      return dateDDMMYYYYFormat;
    } else {
      return dateAnyFormat;
    }
  }

  public void setDate(String date) {
    dateDDMMYYYYFormat = date;
    dateAnyFormat = date;
  }

  public boolean isDDMMYYYY() {
    return dateAnyFormat == null || dateAnyFormat.isEmpty() || isDDMMYYYY;
  }

  public void useDDMMYYYY() {
    useDDMMYYYY = true;
  }

  public void setSummary(String str) {
    summary.override(str, null);
  }

  public boolean isSummaryBeforeTitle() {
    return summaryBeforeTitle;
  }

  private static boolean isImg(String str) {
    return str.endsWith(".jpg")
        || str.endsWith(".jpeg")
        || str.endsWith(".tif")
        || str.endsWith(".tiff")
        || str.endsWith(".svg")
        || str.endsWith(".png")
        || str.endsWith(".gif");
  }

  public void addUrl(Node node, String href, String title, boolean textSibling, boolean anchorSibling, boolean loneBlock, boolean image) {
    ++numUrls;
    String cleanHref = CommonUtil.strip(href, false);
    String cleanTitle = StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeXml(CommonUtil.strip(title, false)));
    cleanTitle = titleJunk.matcher(cleanTitle).replaceAll("");
    String noPunctTitle = cleanTitle.replaceAll("\\p{Punct}", "");
    if (CommonUtil.isEmpty(noPunctTitle)) {
      cleanTitle = "";
    }
    String coreTitle = cleanTitle.replace(" …", "").replace(" ...", "");
    if (!cleanHref.isEmpty()
        && !"#".equals(cleanHref)
        && !cleanTitle.isEmpty()
        && (!coreTitle.contains("/")
            || coreTitle.contains(" ")
            || !coreTitle.contains("."))) {
      int existingScore = 0;
      existingScore += titleHasTextSibling ? 0 : 1;
      existingScore += titleHasAnchorSibling ? 0 : 1;
      existingScore += !titleHasLoneBlock ? 0 : 1;
      existingScore += urlTitle == null || urlTitle.matches("^\\p{Punct}.*$") ? -10 : 0;
      int curScore = 0;
      curScore += textSibling ? 0 : 1;
      curScore += anchorSibling ? 0 : 1;
      curScore += !loneBlock ? 0 : 1;
      curScore += cleanTitle.matches("^\\p{Punct}.*$") ? -10 : 0;
      int curCompare = 0;
      if (url != null) {
        curCompare += cleanTitle.matches("^\\p{Punct}.*$") ? -5 : 0;
        curCompare += urlTitle == null || urlTitle.matches("^\\p{Punct}.*$") ? 9 : 0;
        curCompare += curScore < existingScore ? -2 : curScore > existingScore ? 2 : 0;
        curCompare += curScore <= 1 && curScore <= existingScore ? -1 : 0;
        curCompare += cleanTitle.length() <= urlTitle.length() / 2 ? -2
            : cleanTitle.length() > urlTitle.length() * 2 ? 2 : 0;
        curCompare += cleanHref.contains("#") && !url.contains("#") ? -2 : !cleanHref.contains("#") && url.contains("#") ? 2 : 0;
        int nearestBlock = Util.nearestBlock(node);
        int existingBlock = Util.nearestBlock(urlNodes.get(url));
        curCompare += nearestBlock > existingBlock ? -2
            : nearestBlock == existingBlock ? 0 : 2;
      }
      if (!hasImgUrl
          && CommonUtil.isEmpty(altUrl) && CommonUtil.isEmpty(altUrlTitle)
          && (isImg(cleanTitle) || isImg(cleanHref))) {
        altUrl = cleanHref;
        altUrlTitle = cleanTitle;
        urlNodes.put(cleanHref, node);
        hasImgUrl = true;
      } else if (url == null || !image && titleHasImage || curCompare > 1) {
        if (url != null) {
          priorUrl = url;
          priorUrlTitle = urlTitle;
          addToSummary(urlTitle, true, urlNodes.get(url));
          fallbackUrls.add(url);
          fallbackUrlTitles.add(urlTitle);
        }
        url = cleanHref;
        urlNodes.put(url, node);
        urlTitle = cleanTitle;
        titleHasTextSibling = textSibling;
        titleHasAnchorSibling = anchorSibling;
        titleHasLoneBlock = loneBlock;
        titleHasImage = image;
      } else if (curCompare > -1) {
        fallbackUrls.add(cleanHref);
        urlNodes.put(cleanHref, node);
        fallbackUrlTitles.add(cleanTitle);
        addToSummary(cleanTitle, true, node);
      } else {
        addToSummary(cleanTitle, true, node);
      }
    }
  }

  public void addToSummary(String summary, boolean anchor, Node parent) {
    if (this.summary.contains(parent) || CommonUtil.isEmpty(summary)) {
      return;
    }
    boolean isDate = false;
    if (dateDDMMYYYYFormat == null && likelyDate.matcher(summary).find()) {
      String dateSummary = summary.replaceAll("\\.|-", "/");
      double summaryLen = (double) dateSummary.length();
      Parser parser = new Parser();
      try {
        List<DateGroup> groups = parser.parse(dateSummary);
        if (groups.size() == 1) {
          DateGroup group = groups.get(0);
          List<Date> dateList = group.getDates();
          if (dateList.size() == 1) {
            double dateLen = (double) group.getText().length();
            if (dateLen / summaryLen > DATE_LEN_THRESHOLD) {
              if (group.isTimeInferred()) {
                String formattedDate = dateFormat.format(dateList.get(0));
                dateAnyFormat = formattedDate;
                dateDDMMYYYYFormat = formattedDate;
              } else {
                String formattedDate = dateTimeFormat.format(dateList.get(0)) + " UTC";
                dateAnyFormat = formattedDate;
                dateDDMMYYYYFormat = formattedDate;
              }
              isDate = true;
              textFlagged = false;
            }
          }
        }
      } catch (Throwable t) {}

      Matcher matcher = dateToken.matcher(dateSummary);
      String dateStr = null;
      if (matcher.find()) {
        dateStr = matcher.group(1);
      }
      if (dateStr != null) {
        Date dateTime = null;
        Date date = null;
        try {
          dateTime = dateTimeDDMMYYYY.parse(dateStr);
        } catch (Throwable t) {
          Log.exception(t);
        }
        try {
          date = dateDDMMYYYY.parse(dateStr);
        } catch (Throwable t) {
          Log.exception(t);
        }
        double dateLen = (double) dateStr.length();
        if (dateTime != null && dateLen / summaryLen > DATE_LEN_THRESHOLD) {
          String formattedDate = dateTimeFormat.format(dateTime);
          dateDDMMYYYYFormat = formattedDate;
          isDate = true;
          textFlagged = false;
          isDDMMYYYY = true;
        } else if (date != null && dateLen / summaryLen > DATE_LEN_THRESHOLD) {
          String formattedDate = dateFormat.format(date);
          dateDDMMYYYYFormat = formattedDate;
          isDate = true;
          textFlagged = false;
          isDDMMYYYY = true;
        }
      }
    }
    if (!isDate) {
      String cleanSummary = CommonUtil.strip(summary, true);
      if (cleanSummary.indexOf(' ') != -1
          || (cleanSummary.indexOf('.') == -1 && !textFlagged)
          || cleanSummary.equals("...")) {
        textFlagged = false;
        this.summary.add(summary, parent);
        if (urlTitle == null && !anchor && summary.split("\\s").length > MAX_TOKENS) {
          summaryBeforeTitle = true;
        }
      } else if (cleanSummary.indexOf('.') == -1 && !textFlagged) {
        this.summary.add(summary, parent);
        if (urlTitle == null && !anchor && summary.split("\\s").length > MAX_TOKENS) {
          summaryBeforeTitle = true;
        }
      } else {
        textFlagged = true;
      }
    }
  }
}
