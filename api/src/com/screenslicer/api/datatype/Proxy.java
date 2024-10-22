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
package com.screenslicer.api.datatype;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
    return matchString(proxyA).equals(matchString(proxyB));
  }

  private static final String matchString(Proxy proxy) {
    StringBuilder builder = new StringBuilder();
    builder.append(proxy.ip);
    builder.append("|");
    builder.append(proxy.port);
    builder.append("|");
    builder.append(proxy.username);
    builder.append("|");
    builder.append(proxy.password);
    return builder.toString();
  }

  public static final Map<String, Proxy> toMap(Proxy[] proxies) {
    Map<String, List<Proxy>> map = new HashMap<String, List<Proxy>>();
    map.put(TYPE_DIRECT, new ArrayList<Proxy>());
    map.put(TYPE_ALL, new ArrayList<Proxy>());
    map.put(TYPE_SOCKS_5, new ArrayList<Proxy>());
    map.put(TYPE_SOCKS_4, new ArrayList<Proxy>());
    map.put(TYPE_SOCKS, new ArrayList<Proxy>());
    map.put(TYPE_HTTP, new ArrayList<Proxy>());
    map.put(TYPE_SSL, new ArrayList<Proxy>());
    for (int i = 0; i < proxies.length; i++) {
      map.get(proxies[i].type).add(proxies[i]);
    }
    List<Proxy> toMatch = new ArrayList<Proxy>();
    toMatch.addAll(map.get(TYPE_SOCKS_5));
    toMatch.addAll(map.get(TYPE_SOCKS_4));
    toMatch.addAll(map.get(TYPE_SOCKS));
    toMatch.addAll(map.get(TYPE_HTTP));
    toMatch.addAll(map.get(TYPE_SSL));
    Collection<String> toMatchUnique = new HashSet<String>();
    for (Proxy proxy : toMatch) {
      toMatchUnique.add(matchString(proxy));
    }
    int total = 0;
    total += map.get(TYPE_DIRECT).size();
    total += map.get(TYPE_ALL).size();
    total += toMatchUnique.size();
    int choice = rand.nextInt(total);
    if (choice < map.get(TYPE_DIRECT).size()) {
      return null;
    }
    Map<String, Proxy> ret = new HashMap<String, Proxy>();
    if (choice < map.get(TYPE_ALL).size() + map.get(TYPE_DIRECT).size()) {
      ret.put(TYPE_ALL, map.get(TYPE_ALL).get(rand.nextInt(map.get(TYPE_ALL).size())));
      return ret;
    }
    List<Proxy> socksList = new ArrayList<Proxy>();
    socksList.addAll(map.get(TYPE_SOCKS_5));
    socksList.addAll(map.get(TYPE_SOCKS_4));
    socksList.addAll(map.get(TYPE_SOCKS));
    List<List<Proxy>> proxyLists = new ArrayList<List<Proxy>>();
    proxyLists.add(socksList);
    proxyLists.add(map.get(TYPE_HTTP));
    proxyLists.add(map.get(TYPE_SSL));
    choose(proxyLists.remove(rand.nextInt(proxyLists.size())), ret);
    choose(proxyLists.remove(rand.nextInt(proxyLists.size())), ret);
    choose(proxyLists.remove(rand.nextInt(proxyLists.size())), ret);
    return ret;
  }

  private static void choose(List<Proxy> proxies, Map<String, Proxy> accumulator) {
    if (accumulator != null && proxies != null && !accumulator.isEmpty() && !proxies.isEmpty()) {
      Collection<Proxy> vals = new HashSet<Proxy>(accumulator.values());
      for (Proxy proxy : proxies) {
        for (Proxy val : vals) {
          if (matches(proxy, val)) {
            accumulator.put(proxy.type, proxy);
            return;
          }
        }
      }
    }
    if (accumulator != null && proxies != null && !proxies.isEmpty()) {
      Proxy selected = proxies.get(rand.nextInt(proxies.size()));
      accumulator.put(selected.type, selected);
    }
  }

  /**
   * Socks version 5 proxy
   * 
   * @deprecated will be removed in version 2.0.0.
   */
  public static final String TYPE_SOCKS_5 = "socks5";
  /**
   * Socks version 4 proxy
   * 
   * @deprecated will be removed in version 2.0.0.
   */
  public static final String TYPE_SOCKS_4 = "socks4";
  /**
   * HTTP proxy
   */
  public static final String TYPE_HTTP = "http";
  /**
   * SOCKS proxy
   */
  public static final String TYPE_SOCKS = "socks";
  /**
   * SSL proxy
   * 
   * @deprecated will be removed in version 2.0.0.
   */
  public static final String TYPE_SSL = "ssl";
  /**
   * SOCKSv5, SOCKSv4, HTTP, and SSL (i.e., settings are shared for all
   * protocols)
   * 
   * @deprecated will be removed in version 2.0.0.
   */
  public static final String TYPE_ALL = "all";
  /**
   * Direct (no proxy)
   */
  public static final String TYPE_DIRECT = "direct";
  /**
   * Proxy type.
   * Defaults to Socks.
   */
  public String type = TYPE_SOCKS;
  /**
   * IP address or host of the proxy.
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
