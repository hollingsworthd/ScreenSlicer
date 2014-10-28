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
package com.screenslicer.api.datatype;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.screenslicer.common.CommonUtil;

public class Proxy {
  private static final SecureRandom rand = new SecureRandom();

  public static final Proxy instance(String json) {
    return instance((Map<String, Object>) CommonUtil.gson.fromJson(json, CommonUtil.objectType));
  }

  public static final List<Proxy> instances(String json) {
    return instances((List<Map<String, Object>>) CommonUtil.gson.fromJson(json, CommonUtil.listObjectType));
  }

  public static final Proxy instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Proxy.class, args);
  }

  public static final List<Proxy> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Proxy.class, args);
  }

  public static final List<Proxy> instances(List<Map<String, Object>> args) {
    return CommonUtil.constructListFromMapList(Proxy.class, args);
  }

  public static final String toJson(Proxy obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Proxy>() {}.getType());
  }

  public static final String toJson(Proxy[] obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<Proxy[]>() {}.getType());
  }

  public static final String toJson(List<Proxy> obj) {
    return CommonUtil.gson.toJson(obj, new TypeToken<List<Proxy>>() {}.getType());
  }

  private static final boolean matches(Proxy proxyA, Proxy proxyB) {
    return proxyA.ip.equals(proxyB.ip) && proxyA.port == proxyB.port
        && ((proxyA.username == null && proxyB.username == null)
        || (proxyA.username != null && proxyA.username.equals(proxyB.username)))
        && ((proxyA.password == null && proxyB.password == null)
        || (proxyA.password != null && proxyA.password.equals(proxyB.password)));
  }

  public static final Map<String, Proxy> toMap(Proxy[] proxies) {
    Map<String, List<Proxy>> map = new HashMap<String, List<Proxy>>();
    map.put(TYPE_SOCKS_5, new ArrayList<Proxy>());
    map.put(TYPE_SOCKS_4, new ArrayList<Proxy>());
    map.put(TYPE_HTTP, new ArrayList<Proxy>());
    map.put(TYPE_SSL, new ArrayList<Proxy>());
    map.put(TYPE_ALL, new ArrayList<Proxy>());
    map.put(TYPE_DIRECT, new ArrayList<Proxy>());
    for (int i = 0; i < proxies.length; i++) {
      map.get(proxies[i].type).add(proxies[i]);
    }
    if (!map.get(TYPE_DIRECT).isEmpty()) {
      return null;
    }
    Map<String, Proxy> ret = new HashMap<String, Proxy>();
    if (!map.get(TYPE_ALL).isEmpty()) {
      ret.put(TYPE_ALL, map.get(TYPE_ALL).get(rand.nextInt(map.get(TYPE_ALL).size())));
      return ret;
    }
    List<Proxy> socks = new ArrayList<Proxy>();
    socks.addAll(map.get(TYPE_SOCKS_5));
    socks.addAll(map.get(TYPE_SOCKS_4));
    Proxy selected = socks.isEmpty() ? null : socks.get(rand.nextInt(socks.size()));
    if (selected != null) {
      ret.put(selected.type, selected);
    }
    boolean found = false;
    if (selected != null && !map.get(TYPE_HTTP).isEmpty()) {
      for (Proxy cur : map.get(TYPE_HTTP)) {
        if (matches(cur, selected)) {
          ret.put(TYPE_HTTP, cur);
          found = true;
          break;
        }
      }
    }
    if (!found && !map.get(TYPE_HTTP).isEmpty()) {
      selected = map.get(TYPE_HTTP).get(rand.nextInt(map.get(TYPE_HTTP).size()));
      ret.put(TYPE_HTTP, selected);
    }
    found = false;
    if (selected != null && !map.get(TYPE_SSL).isEmpty()) {
      for (Proxy cur : map.get(TYPE_SSL)) {
        if (matches(cur, selected)) {
          ret.put(TYPE_SSL, cur);
          found = true;
          break;
        }
      }
    }
    if (!found && !map.get(TYPE_SSL).isEmpty()) {
      selected = map.get(TYPE_SSL).get(rand.nextInt(map.get(TYPE_SSL).size()));
      ret.put(TYPE_SSL, selected);
    }
    return ret;
  }

  /**
   * Socks version 5 proxy
   */
  public static final String TYPE_SOCKS_5 = "socks5";
  /**
   * Socks version 4 proxy
   */
  public static final String TYPE_SOCKS_4 = "socks4";
  /**
   * HTTP proxy
   */
  public static final String TYPE_HTTP = "http";
  /**
   * SSL proxy
   */
  public static final String TYPE_SSL = "ssl";
  /**
   * SOCKSv5, SOCKSv4, HTTP, and SSL (i.e., settings are shared for all
   * protocols)
   */
  public static final String TYPE_ALL = "all";
  /**
   * Direct (no proxy)
   */
  public static final String TYPE_DIRECT = "direct";
  /**
   * Proxy type.
   * Defaults to Socks version 5.
   */
  public String type = TYPE_SOCKS_5;
  /**
   * IP address of the proxy.
   * Defaults to 127.0.0.1.
   */
  public String ip = "127.0.0.1";
  /**
   * Port of the proxy.
   * Defaults to 9050.
   */
  public int port = 9050;
  /**
   * Proxy username.
   * Defaults to null (anonymous access).
   */
  public String username;
  /**
   * Proxy password.
   * Defaults to null (anonymous access).
   */
  public String password;
}
