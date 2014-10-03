package com.screenslicer.api.request;

import java.util.List;
import java.util.Map;

import com.screenslicer.common.CommonUtil;

public class Proxy {
  public static final Proxy instance(Map<String, Object> args) {
    return CommonUtil.constructFromMap(Proxy.class, args);
  }

  public static final List<Proxy> instances(Map<String, Object> args) {
    return CommonUtil.constructListFromMap(Proxy.class, args);
  }

  public static final String TYPE_SOCKS_5 = "socks5";
  public static final String TYPE_SOCKS_4 = "socks4";
  public static final String TYPE_HTTP = "http";
  public static final String TYPE_SSL = "ssl";

  public String type = TYPE_SOCKS_5;
  public String ip = "127.0.0.1";
  public int port = 9050;
  public String username = null;
  public String password = null;
}
