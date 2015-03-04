/* 
 * ScreenSlicer (TM) -- automatic, zero-config web scraping (TM)
 * Copyright (C) 2013-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
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
package com.screenslicer.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class CommonUtil {
  static {
    System.setProperty("http.maxConnections", "1024");
  }

  public static void main(String[] args) throws Exception {}

  private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
  static {
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static String asUtc(long timeMs) {
    return format.format(new Date(timeMs));
  }

  public static String asUtc(String timeMs) {
    return format.format(new Date(Long.parseLong(timeMs)));
  }

  public static class MapDateComparator implements Comparator<Object> {
    private final String fieldName;

    public MapDateComparator(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public int compare(Object o1, Object o2) {
      Map<String, Object> lhs = (Map<String, Object>) o1;
      Map<String, Object> rhs = (Map<String, Object>) o2;
      return Long.compare(Long.parseLong((String) rhs.get(fieldName)),
          Long.parseLong((String) lhs.get(fieldName)));
    }
  }

  public static String compress(String string) {
    if (string == null) {
      return null;
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(out) {
        {
          def.setLevel(Deflater.BEST_COMPRESSION);
        }
      };
      gzip.write(string.getBytes("utf-8"));
      gzip.close();
      return Base64.encodeBase64String(out.toByteArray());
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  public static String decompress(String string) {
    if (string == null) {
      return null;
    }
    try {
      GZIPInputStream in = new GZIPInputStream(
          new ByteArrayInputStream(Base64.decodeBase64(string.getBytes("utf-8"))));
      return IOUtils.toString(in, "utf-8");
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  static String myInstance = null;

  public static String myInstance() {
    if (myInstance != null) {
      return myInstance;
    }
    if (!CommonUtil.isEmpty(Config.instance.myInstance())) {
      myInstance = Config.instance.myInstance();
      return myInstance;
    }
    try {
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces.hasMoreElements()) {
        NetworkInterface iface = ifaces.nextElement();
        Enumeration<InetAddress> addrs = iface.getInetAddresses();
        while (addrs.hasMoreElements()) {
          InetAddress addr = addrs.nextElement();
          String ip = addr.getHostAddress();
          if (ip.contains(".")
              && !ip.startsWith("127.")
              && !ip.startsWith("192.168.")
              && !ip.startsWith("10.")
              && !ip.startsWith("172.16.")
              && !ip.startsWith("172.17.")
              && !ip.startsWith("172.18.")
              && !ip.startsWith("172.19.")
              && !ip.startsWith("172.20.")
              && !ip.startsWith("172.21.")
              && !ip.startsWith("172.22.")
              && !ip.startsWith("172.23.")
              && !ip.startsWith("172.24.")
              && !ip.startsWith("172.25.")
              && !ip.startsWith("172.26.")
              && !ip.startsWith("172.27.")
              && !ip.startsWith("172.28.")
              && !ip.startsWith("172.29.")
              && !ip.startsWith("172.30.")
              && !ip.startsWith("172.31.")
              && !ip.startsWith("169.254.")
              && !ip.startsWith("224.")
              && !ip.startsWith("225.")
              && !ip.startsWith("226.")
              && !ip.startsWith("227.")
              && !ip.startsWith("228.")
              && !ip.startsWith("229.")
              && !ip.startsWith("230.")
              && !ip.startsWith("231.")
              && !ip.startsWith("232.")
              && !ip.startsWith("233.")
              && !ip.startsWith("234.")
              && !ip.startsWith("235.")
              && !ip.startsWith("236.")
              && !ip.startsWith("237.")
              && !ip.startsWith("238.")
              && !ip.startsWith("239.")
              && !ip.startsWith("255.255.255.255")) {
            return ip;
          }
        }
      }
    } catch (SocketException e) {
      Log.exception(e);
    }
    return "127.0.0.1";
  }

  public static void sendEmail(String[] recipients, String subject,
      String body, Map<String, byte[]> attachments) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("key", Config.instance.mandrillKey());
    List<Map<String, String>> to = new ArrayList<Map<String, String>>();
    for (int i = 0; i < recipients.length; i++) {
      to.add(CommonUtil.asMap("email", "name", "type",
          recipients[i], recipients[i].split("@")[0], "to"));
    }
    List<Map<String, String>> attachmentList = null;
    if (attachments != null) {
      attachmentList = new ArrayList<Map<String, String>>();
      for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
        attachmentList.add(CommonUtil.asMap("type", "name", "content",
            new Tika().detect(entry.getValue()), entry.getKey(),
            Base64.encodeBase64String(entry.getValue())));
      }
    }
    params.put("message", CommonUtil.asObjMap(
        "track_clicks", "track_opens", "html",
        "headers", "subject",
        "from_email", "from_name", "to", "attachments",
        false, false, body,
        CommonUtil.asMap("Reply-To", Config.instance.mandrillEmail()), subject,
        Config.instance.mandrillEmail(), Config.instance.mandrillEmail(), to, attachmentList));
    params.put("async", true);
    HttpURLConnection conn = null;
    String resp = null;
    Log.info("Sending email: " + subject, false);
    try {
      conn = (HttpURLConnection) new URL(
          "https://mandrillapp.com/api/1.0/messages/send.json").openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("User-Agent", "Mandrill-Curl/1.0");
      String data = CommonUtil.gson.toJson(params, CommonUtil.objectType);
      byte[] bytes = data.getBytes("utf-8");
      conn.setRequestProperty("Content-Length", "" + bytes.length);
      OutputStream os = conn.getOutputStream();
      os.write(bytes);
      conn.connect();
      resp = IOUtils.toString(conn.getInputStream(), "utf-8");
      if (resp.contains("\"rejected\"") || resp.contains("\"invalid\"")) {
        Log.warn("Invalid/rejected email addreses");
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public static int nextTextBreak(String str, int start) {
    if (CommonUtil.isEmpty(str) || start >= str.length()) {
      return str == null ? -1 : str.length();
    }
    int next = start;
    for (; next + 1 < str.length()
        && (str.charAt(next) != '.'
        || !Character.isWhitespace(str.charAt(next + 1)))
        && str.charAt(next) != '\n'
        && str.charAt(next) != '\r'
        && str.charAt(next) != '\f'; next++);
    return (str.charAt(next) == '\n'
        || str.charAt(next) == '\r'
        || str.charAt(next) == '\f') ? next : next + 1;
  }

  public static String toString(Collection<?> strings, String delim) {
    if (strings == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    List<String> stringList = new ArrayList<String>();
    for (Object str : strings) {
      String val = str == null ? "" : str.toString();
      val = val == null ? "" : val;
      stringList.add(val);
    }
    for (int i = 0; i < stringList.size(); i++) {
      builder.append(stringList.get(i));
      if (i + 1 < stringList.size()) {
        builder.append(delim);
      }
    }
    return builder.toString();
  }

  public static String toString(String[] strings, String delim) {
    if (isEmpty(strings)) {
      return "";
    }
    return toString(Arrays.asList(strings), delim);
  }

  public static String asHash(Map<String, Object> map) {
    StringBuilder builder = new StringBuilder();
    String[] entries = new String[map.size()];
    int cur = 0;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String value = "";
      if (entry.getValue() instanceof String) {
        value = (String) entry.getValue();
      }
      entries[cur++] = entry.getKey() + ":" + value + ",";
    }
    Arrays.sort(entries);
    for (int i = 0; i < entries.length; i++) {
      builder.append(entries[i]);
    }
    return DigestUtils.sha256Hex(builder.toString());
  }

  public static <T extends Object> T constructFromMap(Class<T> classOf, Map<String, Object> args) {
    return CommonUtil.gson.fromJson(CommonUtil.gson.toJson(args, CommonUtil.objectType), classOf);
  }

  public static <T extends Object> List<T> constructListFromMapList(Class<T> classOf, List<Map<String, Object>> args) {
    List<T> list = new ArrayList<T>();
    for (Map<String, Object> arg : args) {
      list.add(CommonUtil.gson.fromJson(CommonUtil.gson.toJson(arg, CommonUtil.objectType), classOf));
    }
    return list;
  }

  public static String combinedJson(Object obj1, Object obj2) {
    Map<String, Object> map1 = CommonUtil.gson.fromJson(CommonUtil.gson.toJson(obj1, obj1.getClass()), CommonUtil.objectType);
    Map<String, Object> map2 = CommonUtil.gson.fromJson(CommonUtil.gson.toJson(obj2, obj2.getClass()), CommonUtil.objectType);
    map1.putAll(map2);
    return CommonUtil.gson.toJson(map1, CommonUtil.objectType);
  }

  public static int min(int... ints) {
    int min = Integer.MAX_VALUE;
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] < min) {
        min = ints[i];
      }
    }
    return min;
  }

  public static int max(int... ints) {
    int max = Integer.MIN_VALUE;
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] > max) {
        max = ints[i];
      }
    }
    return max;
  }

  public static int max(List<Integer> ints) {
    int max = Integer.MIN_VALUE;
    for (Integer curInt : ints) {
      if (curInt.intValue() > max) {
        max = curInt.intValue();
      }
    }
    return max;
  }

  public static Document parse(String string, String url, boolean ascii) {
    Document doc = url == null ? Jsoup.parse(string) : Jsoup.parse(string, url);
    sanitize(doc, ascii);
    return doc;
  }

  public static Element parseFragment(String string, boolean ascii) {
    Document doc = Jsoup.parseBodyFragment(string);
    sanitize(doc, ascii);
    if (!doc.body().children().isEmpty()) {
      return doc.body().child(0);
    }
    return doc.body();
  }

  private static Element sanitize(Document doc, final boolean ascii) {
    if (ascii) {
      doc.outputSettings().charset("ascii");
    } else {
      doc.outputSettings().charset("utf-8");
    }
    doc.traverse(new NodeVisitor() {
      @Override
      public void tail(Node n, int d) {}

      @Override
      public void head(Node n, int d) {
        try {
          if (n.nodeName().equals("#text")
              && !CommonUtil.isEmpty(n.outerHtml())) {
            ((TextNode) n).text(HtmlCoder.decode(n.toString()));
          }
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });
    return doc;
  }

  public static <T extends Object> List<T> constructListFromMap(Class<T> classOf, Map<String, Object> args) {
    Field[] fields = classOf.getFields();
    int len = 0;
    for (int i = 0; i < fields.length; i++) {
      for (int j = 0; j < args.size() * 2; j++) {
        if (args.containsKey(fields[i].getName() + j)
            && j > len) {
          len = j;
        }
      }
    }
    ++len;
    List<Map<String, Object>> argsList = new ArrayList<Map<String, Object>>(len);
    for (int i = 0; i < fields.length; i++) {
      for (int j = 0; j < len; j++) {
        if (argsList.size() == j || argsList.get(j) == null) {
          argsList.add(new HashMap<String, Object>());
        }
        String name = fields[i].getName();
        String name0 = name + "0";
        String key = name + j;
        if (args.containsKey(name0)) {
          if (args.containsKey(key)) {
            argsList.get(j).put(name, args.get(key));
          } else if (args.containsKey(name)) {
            argsList.get(j).put(name, args.get(name));
          } else {
            argsList.get(j).put(name, args.get(name0));
          }
        } else {
          argsList.get(j).put(name, args.get(name));
        }
      }
    }
    List<T> list = new ArrayList<T>();
    for (Map<String, Object> map : argsList) {
      list.add(CommonUtil.gson.fromJson(CommonUtil.gson.toJson(map), classOf));
    }
    return list;
  }

  public static String get(MultivaluedMap<String, String> map, String key) {
    List<String> vals = map.get(key);
    if (vals != null) {
      return (String) map.get(key).get(0);
    }
    return null;
  }

  public static String getObj(MultivaluedMap<String, String> map, String key) {
    List<String> vals = map.get(key);
    if (vals != null) {
      return (String) map.get(key).get(0);
    }
    return null;
  }

  public static int intVal(Object o) {
    if (o instanceof Integer) {
      return ((Integer) o).intValue();
    } else if (o instanceof Double) {
      return (int) ((Double) o).doubleValue();
    } else if (o instanceof Long) {
      return (int) ((Long) o).longValue();
    }
    return 0;
  }

  public static boolean isEmpty(Object[] objects) {
    return objects == null || objects.length == 0;
  }

  public static boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }

  public static boolean equals(String str1, String str2) {
    return (isEmpty(str1) && isEmpty(str2))
        || (!isEmpty(str1) && str1.equals(str2));
  }

  public static boolean isEmpty(String str, boolean strip) {
    return str == null || str.trim().isEmpty() || (strip && strip(str, false).isEmpty());
  }

  public static boolean isEmpty(Map<? extends Object, ? extends Object> map) {
    return map == null || map.isEmpty();
  }

  public static boolean isEmpty(List<? extends Object> list) {
    return list == null
        || list.isEmpty()
        || (list.size() == 1
        && (list.get(0) == null
        || (list.get(0) instanceof String && CommonUtil.isEmpty(list.get(0).toString()))));
  }

  public static Map<String, Object> asObjMap(Object... keysAndVals) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    int mid = keysAndVals.length / 2;
    for (int i = 0; i < mid; i++) {
      map.put((String) keysAndVals[i], keysAndVals[i + mid]);
    }
    return map;
  }

  public static Map<String, List<List<String>>> asTableMap(Object... keysAndVals) {
    Map<String, List<List<String>>> map = new LinkedHashMap<String, List<List<String>>>();
    int mid = keysAndVals.length / 2;
    for (int i = 0; i < mid; i++) {
      map.put((String) keysAndVals[i], (List<List<String>>) keysAndVals[i + mid]);
    }
    return map;
  }

  public static Map<String, List<Map<String, Object>>> asJsonTableMap(Object... keysAndVals) {
    Map<String, List<Map<String, Object>>> map = new LinkedHashMap<String, List<Map<String, Object>>>();
    int mid = keysAndVals.length / 2;
    for (int i = 0; i < mid; i++) {
      map.put((String) keysAndVals[i], (List<Map<String, Object>>) keysAndVals[i + mid]);
    }
    return map;
  }

  public static Map<String, String> asMap(String... keysAndVals) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    int mid = keysAndVals.length / 2;
    for (int i = 0; i < mid; i++) {
      map.put(keysAndVals[i], keysAndVals[i + mid]);
    }
    return map;
  }

  public static final Type listStringType = new TypeToken<List<Map<String, String>>>() {}.getType();
  public static final Type listObjectType = new TypeToken<List<Map<String, Object>>>() {}.getType();
  public static final Type stringType = new TypeToken<Map<String, String>>() {}.getType();
  public static final Type stringArrayType = new TypeToken<String[]>() {}.getType();
  public static final Type objectType = new TypeToken<Map<String, Object>>() {}.getType();
  public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
  public static final String BUSY = "BUSY";
  public static final String NOT_BUSY = "NOT_BUSY";
  private static final AsyncHttpClient client = new AsyncHttpClient(LenientHttpsConfig.instance());
  private static final String user = Config.instance.basicAuthUser();
  private static final String pass = Config.instance.basicAuthPass();

  public static Response internalHttpCall(String ip, String uri, String method,
      Map<String, String> headers, byte[] postData, String workingDir) {
    return (Response) internalHttpCall(ip, uri, method, headers, postData, workingDir, false);
  }

  public static String internalHttpStrCall(String ip, String uri, String method,
      Map<String, String> headers, byte[] postData, String workingDir) {
    return (String) internalHttpCall(ip, uri, method, headers, postData, workingDir, true);
  }

  public static boolean isUrl(String str) {
    try {
      str = str.replaceAll("\\s", "%20");
      return (str.startsWith("http://") || str.startsWith("https://"))
          && new URI(str) != null;
    } catch (Throwable t) {}
    return false;
  }

  private static Object internalHttpCall(String ip, String uri, String method,
      Map<String, String> headers, byte[] postData, String workingDir, boolean stringResp) {
    try {
      workingDir = workingDir == null ? "" : workingDir;
      BoundRequestBuilder req = null;
      if ("get".equalsIgnoreCase(method)) {
        req = client.prepareGet(uri);
      } else if ("post".equalsIgnoreCase(method)) {
        req = client.preparePost(uri);
      } else if ("put".equalsIgnoreCase(method)) {
        req = client.preparePut(uri);
      } else if ("delete".equalsIgnoreCase(method)) {
        req = client.prepareDelete(uri);
      } else if ("connect".equalsIgnoreCase(method)) {
        req = client.prepareConnect(uri);
      } else if ("head".equalsIgnoreCase(method)) {
        req = client.prepareHead(uri);
      } else if ("options".equalsIgnoreCase(method)) {
        req = client.prepareOptions(uri);
      } else {
        throw new IllegalArgumentException("Invalid HTTP method specified: " + method);
      }
      req.setRequestTimeout(0);
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          req.setHeader(entry.getKey(), entry.getValue());
        }
      }
      if (!CommonUtil.isEmpty(user) || !CommonUtil.isEmpty(pass)) {
        String myUser = user == null ? "" : user;
        String myPass = pass == null ? "" : pass;
        req.setHeader("Authorization",
            "Basic " + Base64.encodeBase64String((myUser + ":" + myPass).getBytes("utf-8")));
      }
      if (postData != null && postData.length > 0) {
        req.setContentLength(postData.length);
        req.setBody(postData);
      }
      com.ning.http.client.Response resp = req.execute().get();
      byte[] content = null;
      try {
        if (stringResp) {
          return resp.getResponseBody();
        }
        content = resp.getResponseBodyAsBytes();
      } catch (Throwable t) {
        Log.exception(t, uri);
        content = null;
      }
      if (resp.getStatusCode() == 302) {
        return Response.seeOther(URI.create(workingDir + resp.getHeader("Location"))).build();
      } else {
        ResponseBuilder myResp = null;
        if (content == null || content.length == 0) {
          myResp = Response.ok();
        } else {
          myResp = Response.ok(content);
        }
        for (Map.Entry<String, List<String>> entry : resp.getHeaders().entrySet()) {
          if (!CommonUtil.isEmpty(entry.getKey())) {
            myResp = myResp.header(entry.getKey(), CommonUtil.toString(entry.getValue(), ","));
          }
        }
        myResp = myResp.status(resp.getStatusCode());
        return myResp.build();
      }
    } catch (Throwable t) {
      Log.exception(t, uri);
    }
    return null;
  }

  public static String post(String uri, String recipient, String postData) {
    HttpURLConnection conn = null;
    try {
      postData = Crypto.encode(postData, recipient);
      conn = (HttpURLConnection) new URL(uri).openConnection();
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      conn.setRequestMethod("POST");
      conn.setConnectTimeout(0);
      conn.setRequestProperty("Content-Type", "application/json");
      byte[] bytes = postData.getBytes("utf-8");
      conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
      OutputStream os = conn.getOutputStream();
      os.write(bytes);
      conn.connect();
      if (conn.getResponseCode() == 205) {
        return NOT_BUSY;
      }
      if (conn.getResponseCode() == 423 || conn.getResponseCode() == 202) {
        return BUSY;
      }
      return Crypto.decode(IOUtils.toString(conn.getInputStream(), "utf-8"), recipient);
    } catch (Throwable t) {
      Log.exception(t);
    }
    return "";
  }

  public static void postQuickly(String uri, String recipient, String postData) {
    HttpURLConnection conn = null;
    try {
      postData = Crypto.encode(postData, recipient);
      conn = (HttpURLConnection) new URL(uri).openConnection();
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      conn.setRequestMethod("POST");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      conn.setRequestProperty("Content-Type", "application/json");
      byte[] bytes = postData.getBytes("utf-8");
      conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
      OutputStream os = conn.getOutputStream();
      os.write(bytes);
      conn.connect();
      Crypto.decode(IOUtils.toString(conn.getInputStream(), "utf-8"), recipient);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public static Element getElementByClassName(Element doc, String tagName, String className) {
    if (doc == null) {
      return null;
    }
    Elements elements = doc.getElementsByClass(className);
    for (Element element : elements) {
      if (element.tagName().equalsIgnoreCase(tagName)) {
        return element;
      }
    }
    return null;
  }

  public static Element getElementMatchingText(Element doc, String tagName, String pattern, boolean ownText) {
    if (doc == null) {
      return null;
    }
    Elements elements = ownText ? doc.getElementsMatchingOwnText(pattern)
        : doc.getElementsMatchingText(pattern);
    for (Element element : elements) {
      if (element.tagName().equalsIgnoreCase(tagName)) {
        return element;
      }
    }
    return null;
  }

  public static Element getElementByTagName(Elements elements, String tagName) {
    if (elements == null) {
      return null;
    }
    for (Element element : elements) {
      if (element.tagName().equalsIgnoreCase(tagName)) {
        return element;
      }
    }
    return null;
  }

  public static String getFirstChildTextByTagName(Elements elements, String tagName) {
    if (elements == null) {
      return null;
    }
    if (elements.isEmpty()) {
      return null;
    }
    Element element = elements.get(0);
    for (Element child : element.children()) {
      if (child.tagName().equalsIgnoreCase(tagName)) {
        return child.text();
      }
    }
    return null;
  }

  public static String getNextSiblingTextByOwnText(Element doc, String text) {
    Elements elements = doc.getElementsMatchingOwnText(text);
    if (elements == null) {
      return null;
    }
    if (elements.isEmpty()) {
      return null;
    }
    Element sibling = elements.get(0).nextElementSibling();
    if (sibling != null) {
      return sibling.text();
    }
    return null;
  }

  public static List<Element> getNextSiblingElementsByOwnText(Element doc, String text) {
    Elements elements = doc.getElementsMatchingOwnText(text);
    List<Element> siblings = new ArrayList<Element>();
    if (elements == null || elements.isEmpty()) {
      return siblings;
    }
    Element element = elements.get(0).nextElementSibling();
    while (element != null) {
      siblings.add(element);
      String tagName = element.tagName();
      element = element.nextElementSibling();
      if (element != null && !element.tagName().equalsIgnoreCase(tagName)) {
        break;
      }
    }
    return siblings;
  }

  public static Pattern whitespace = Pattern
      .compile(
          "(?:\\p{javaWhitespace}|\\p{Cntrl}|[\\u0009\\u000A\\u000B\\u000C\\u000D\\u00A0\\u0020\\u0085\\u00A0\\u1680\\u180E\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]|&(amp;)?(?:#(?:x(?:00a0|20(?:0(?:[239cd])|2[89]))|(?:160|8(?:19[45]|2(?:0[145]|3[23]))))|(?:nb|thin|e[mn])sp);|\\|)+",
          Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern whitespaceWithPipes = Pattern
      .compile(
          "(?:\\p{javaWhitespace}|\\p{Cntrl}|[\\u0009\\u000A\\u000B\\u000C\\u000D\\u00A0\\u0020\\u0085\\u00A0\\u1680\\u180E\\u2000\\u2001\\u2002\\u2003\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]|&(amp;)?(?:#(?:x(?:00a0|20(?:0(?:[239cd])|2[89]))|(?:160|8(?:19[45]|2(?:0[145]|3[23]))))|(?:nb|thin|e[mn])sp);)+",
          Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
  public static Pattern newlines = Pattern.compile("\r\n|[\r\n]");
  private static final int MAX_STRIP_CACHE = 5000;
  private static final Map<Integer, String> stripCacheWithPipes = new HashMap<Integer, String>(MAX_STRIP_CACHE);
  private static final Map<Integer, String> stripCache = new HashMap<Integer, String>(MAX_STRIP_CACHE);

  public static String stripAttributes(String string) {
    return string.replaceAll("<\\s*(/?\\w+)(\\s*[^>]*)>", "<$1>");
  }

  public static String strip(String str, boolean removePipes) {
    if (str == null) {
      return "";
    }
    int hash = Crypto.fastHashInt(str);
    Map<Integer, String> cache = removePipes ? stripCache : stripCacheWithPipes;
    if (cache.containsKey(hash)) {
      return cache.get(hash);
    }
    Pattern pattern = removePipes ? CommonUtil.whitespace : CommonUtil.whitespaceWithPipes;
    String oldStr = str;
    String newStr = pattern.matcher(oldStr).replaceAll(" ");
    while (!newStr.equals(oldStr)) {
      oldStr = newStr;
      newStr = pattern.matcher(oldStr).replaceAll(" ");
    }
    newStr = newStr.trim();
    if (cache.size() == MAX_STRIP_CACHE) {
      cache.clear();
    }
    cache.put(hash, newStr);
    return newStr;
  }

  public static String trim(String str) {
    if (isEmpty(str)) {
      return str;
    }
    return str.trim();
  }

  public static String stripNewlines(String str) {
    if (str == null) {
      return str;
    }
    if (str.trim().isEmpty()) {
      return str.trim();
    }
    String oldStr = str;
    String newStr = newlines.matcher(oldStr).replaceAll("");
    while (!newStr.equals(oldStr)) {
      oldStr = newStr;
      newStr = newlines.matcher(oldStr).replaceAll("");
    }
    return newStr.trim();
  }
}
